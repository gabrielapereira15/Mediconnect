package com.vegs.mediconnect.fhir;

import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Turns this application's own entities into FHIR R4 resources.
 *
 * Kept apart from the entities so the database schema is not dragged around
 * by the standard, and apart from the controllers so the same mapping serves
 * both the REST endpoints and the patient summary.
 *
 * Where a CA Baseline or PS-CA profile exists for a resource, it is stamped
 * into meta.profile — that claim is what the conformance tests check.
 */
@Component
@RequiredArgsConstructor
public class FhirMapper {

    /** The clinic books in half-hour slots. */
    private static final int SLOT_MINUTES = 30;

    private final FhirProperties properties;

    // ---- Patient --------------------------------------------------------

    public org.hl7.fhir.r4.model.Patient toPatient(Patient patient) {
        var fhir = new org.hl7.fhir.r4.model.Patient();
        fhir.setId(patient.getId().toString());
        fhir.getMeta().addProfile(FhirProfiles.CA_PATIENT);

        // The clinic's own record number, always present.
        fhir.addIdentifier()
                .setSystem(properties.getLocalPatientSystem())
                .setValue(patient.getId().toString());

        // The health card, when we have one and know which province issued
        // it. CA Baseline slices on this identifier, so the type coding
        // matters as much as the number.
        String system = properties.healthCardSystem(patient.getHealthCardProvince());
        if (system != null && notBlank(patient.getHealthCardNumber())) {
            fhir.addIdentifier()
                    .setType(new CodeableConcept().addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                            .setCode("JHN")
                            .setDisplay("Jurisdictional health number")))
                    .setSystem(system)
                    .setValue(patient.getHealthCardNumber());
        }

        fhir.setActive(true);
        fhir.addName()
                .setUse(HumanName.NameUse.OFFICIAL)
                .setFamily(patient.getLastName())
                .addGiven(patient.getFirstName());

        if (notBlank(patient.getEmail())) {
            fhir.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(patient.getEmail());
        }
        if (notBlank(patient.getPhoneNumber())) {
            fhir.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(patient.getPhoneNumber());
        }
        if (notBlank(patient.getAddress())) {
            fhir.addAddress().addLine(patient.getAddress()).setCountry("CA");
        }

        applyGender(fhir, patient.getGender());
        applyBirthDate(fhir, patient.getBirthdate());

        return fhir;
    }

    private void applyGender(org.hl7.fhir.r4.model.Patient fhir, String gender) {
        if (!notBlank(gender)) {
            return;
        }
        // The app collects free text, so anything unrecognised becomes
        // "other" rather than being dropped or guessed at.
        switch (gender.trim().toLowerCase()) {
            case "male", "m" -> fhir.setGender(Enumerations.AdministrativeGender.MALE);
            case "female", "f" -> fhir.setGender(Enumerations.AdministrativeGender.FEMALE);
            case "unknown" -> fhir.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            default -> fhir.setGender(Enumerations.AdministrativeGender.OTHER);
        }
    }

    private void applyBirthDate(org.hl7.fhir.r4.model.Patient fhir, String birthdate) {
        if (!notBlank(birthdate)) {
            return;
        }
        try {
            fhir.setBirthDate(toDate(LocalDate.parse(birthdate.trim())));
        } catch (Exception ignored) {
            // Historically stored as free text, so an unreadable one is left
            // off rather than allowed to fail the whole resource.
        }
    }

    // ---- Practitioner ---------------------------------------------------

    public Practitioner toPractitioner(Doctor doctor) {
        var fhir = new Practitioner();
        fhir.setId(doctor.getId().toString());
        fhir.getMeta().addProfile(FhirProfiles.CA_PRACTITIONER);

        fhir.addIdentifier()
                .setSystem(properties.getPractitionerSystem())
                .setValue(doctor.getId().toString());

        fhir.setActive(true);
        fhir.addName()
                .setUse(HumanName.NameUse.OFFICIAL)
                .setFamily(doctor.getLastName())
                .addGiven(doctor.getFirstName())
                .addPrefix("Dr.");

        // No contact details: the clinic's doctors are reached through the
        // clinic, and publishing a personal address in an exportable
        // resource would be a privacy decision nobody has made.
        return fhir;
    }

    /**
     * The doctor's specialty, which in FHIR belongs to the role they hold at
     * an organisation rather than to the person.
     */
    public PractitionerRole toPractitionerRole(Doctor doctor) {
        var role = new PractitionerRole();
        role.setId(doctor.getId().toString());
        role.getMeta().addProfile(FhirProfiles.CA_PRACTITIONER_ROLE);
        role.setActive(true);
        role.setPractitioner(new Reference("Practitioner/" + doctor.getId()));

        if (notBlank(doctor.getSpecialty())) {
            // Free text: mapping a clinic's specialty names onto SNOMED needs
            // a terminology service, and a wrong code is worse than an honest
            // uncoded one.
            role.addSpecialty().setText(doctor.getSpecialty());
        }
        return role;
    }

    // ---- Scheduling -----------------------------------------------------
    //
    // Neither CA Baseline nor PS-CA profiles Appointment, Schedule or Slot,
    // so these are plain FHIR R4 with no meta.profile. Claiming a Canadian
    // profile here would mean inventing one.

    public Slot toSlot(ScheduleTime scheduleTime) {
        var slot = new Slot();
        slot.setId(scheduleTime.getId().toString());
        slot.setSchedule(new Reference("Schedule/" + scheduleTime.getSchedule().getId()));
        slot.setStatus(Boolean.TRUE.equals(scheduleTime.getAvailable())
                ? Slot.SlotStatus.FREE
                : Slot.SlotStatus.BUSY);

        LocalDateTime start = LocalDateTime.of(
                scheduleTime.getSchedule().getDate(), scheduleTime.getTime());
        slot.setStart(toDate(start));
        slot.setEnd(toDate(start.plusMinutes(SLOT_MINUTES)));
        return slot;
    }

    public Schedule toSchedule(com.vegs.mediconnect.datasource.schedule.Schedule schedule) {
        var fhir = new Schedule();
        fhir.setId(schedule.getId().toString());
        fhir.setActive(Boolean.TRUE.equals(schedule.getAvailable()));
        fhir.addActor(new Reference("Practitioner/" + schedule.getDoctor().getId()));
        return fhir;
    }

    public org.hl7.fhir.r4.model.Appointment toAppointment(Appointment appointment) {
        var fhir = new org.hl7.fhir.r4.model.Appointment();
        fhir.setId(appointment.getId().toString());

        var scheduleTime = appointment.getScheduleTime();
        LocalDateTime start = LocalDateTime.of(
                scheduleTime.getSchedule().getDate(), scheduleTime.getTime());
        fhir.setStart(toDate(start));
        fhir.setEnd(toDate(start.plusMinutes(SLOT_MINUTES)));
        fhir.setStatus(appointmentStatus(appointment, start));
        fhir.addSlot(new Reference("Slot/" + scheduleTime.getId()));

        addSubject(fhir, appointment);
        fhir.addParticipant()
                .setActor(new Reference("Practitioner/" + appointment.getDoctor().getId()))
                .setRequired(org.hl7.fhir.r4.model.Appointment.ParticipantRequired.REQUIRED)
                .setStatus(org.hl7.fhir.r4.model.Appointment.ParticipationStatus.ACCEPTED);

        return fhir;
    }

    /**
     * Names who is actually attending.
     *
     * When the visit was booked for someone else, that person is the subject
     * and the account holder is listed separately as the one who arranged it
     * — which is how FHIR expresses a parent booking for a child.
     */
    private void addSubject(org.hl7.fhir.r4.model.Appointment fhir, Appointment appointment) {
        String bookedFor = appointment.getBookedForName();
        Reference accountHolder = new Reference("Patient/" + appointment.getPatient().getId());

        if (bookedFor == null || bookedFor.isBlank()) {
            fhir.addParticipant()
                    .setActor(accountHolder)
                    .setRequired(org.hl7.fhir.r4.model.Appointment.ParticipantRequired.REQUIRED)
                    .setStatus(org.hl7.fhir.r4.model.Appointment.ParticipationStatus.ACCEPTED);
            return;
        }

        // The dependant has no record of their own, so they are carried as a
        // display-only reference rather than a link to a Patient that does
        // not exist.
        fhir.addParticipant()
                .setActor(new Reference().setDisplay(bookedFor))
                .setRequired(org.hl7.fhir.r4.model.Appointment.ParticipantRequired.REQUIRED)
                .setStatus(org.hl7.fhir.r4.model.Appointment.ParticipationStatus.ACCEPTED)
                .addType(new CodeableConcept().addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType")
                        .setCode("SBJ")
                        .setDisplay("subject")));
        fhir.addParticipant()
                .setActor(accountHolder)
                .setRequired(org.hl7.fhir.r4.model.Appointment.ParticipantRequired.INFORMATIONONLY)
                .setStatus(org.hl7.fhir.r4.model.Appointment.ParticipationStatus.ACCEPTED);
    }

    private org.hl7.fhir.r4.model.Appointment.AppointmentStatus appointmentStatus(
            Appointment appointment, LocalDateTime start) {
        if (Boolean.TRUE.equals(appointment.getCanceled())) {
            return org.hl7.fhir.r4.model.Appointment.AppointmentStatus.CANCELLED;
        }
        if (start.toLocalDate().isBefore(LocalDate.now())) {
            return org.hl7.fhir.r4.model.Appointment.AppointmentStatus.FULFILLED;
        }
        return org.hl7.fhir.r4.model.Appointment.AppointmentStatus.BOOKED;
    }

    // ---- Health record entries ------------------------------------------

    public AllergyIntolerance toAllergyIntolerance(HealthEntry entry) {
        var allergy = new AllergyIntolerance();
        allergy.setId(entry.getId().toString());
        allergy.getMeta().addProfile(FhirProfiles.PSCA_ALLERGY);

        allergy.setClinicalStatus(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                .setCode(entry.isActive() ? "active" : "inactive")));
        allergy.setVerificationStatus(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                // Reported by the patient and not yet confirmed by a
                // clinician, which is exactly what "unconfirmed" means.
                .setCode("unconfirmed")));

        allergy.setCode(codeableConcept(entry));
        allergy.setPatient(new Reference("Patient/" + entry.getPatient().getId()));
        if (entry.getOnsetDate() != null) {
            allergy.setOnset(new DateTimeType(toDate(entry.getOnsetDate())));
        }
        if (notBlank(entry.getNote())) {
            allergy.addNote().setText(entry.getNote());
        }
        return allergy;
    }

    public Condition toCondition(HealthEntry entry) {
        var condition = new Condition();
        condition.setId(entry.getId().toString());
        condition.getMeta().addProfile(FhirProfiles.PSCA_CONDITION);

        condition.setClinicalStatus(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode(entry.isActive() ? "active" : "resolved")));
        condition.setVerificationStatus(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("unconfirmed")));

        condition.setCode(codeableConcept(entry));
        condition.setSubject(new Reference("Patient/" + entry.getPatient().getId()));
        if (entry.getOnsetDate() != null) {
            condition.setOnset(new DateTimeType(toDate(entry.getOnsetDate())));
        }
        if (notBlank(entry.getNote())) {
            condition.addNote().setText(entry.getNote());
        }
        return condition;
    }

    public MedicationStatement toMedicationStatement(HealthEntry entry) {
        var statement = new MedicationStatement();
        statement.setId(entry.getId().toString());
        statement.getMeta().addProfile(FhirProfiles.PSCA_MEDICATION_STATEMENT);

        statement.setStatus(entry.isActive()
                ? MedicationStatement.MedicationStatementStatus.ACTIVE
                : MedicationStatement.MedicationStatementStatus.STOPPED);
        statement.setMedication(codeableConcept(entry));
        statement.setSubject(new Reference("Patient/" + entry.getPatient().getId()));
        if (entry.getOnsetDate() != null) {
            statement.setEffective(new DateTimeType(toDate(entry.getOnsetDate())));
        }
        if (notBlank(entry.getNote())) {
            statement.addNote().setText(entry.getNote());
        }
        return statement;
    }

    /**
     * The patient's own words, plus a coding only when one has actually been
     * assigned. A CodeableConcept carrying just text is valid and honest; a
     * guessed SNOMED code would not be.
     */
    private CodeableConcept codeableConcept(HealthEntry entry) {
        var concept = new CodeableConcept().setText(entry.getDescription());
        if (notBlank(entry.getCode()) && notBlank(entry.getCodeSystem())) {
            concept.addCoding(new Coding()
                    .setSystem(entry.getCodeSystem())
                    .setCode(entry.getCode())
                    .setDisplay(entry.getDescription()));
        }
        return concept;
    }

    // ---- helpers --------------------------------------------------------

    public Organization organization() {
        var organization = new Organization();
        organization.setId("mediconnect");
        organization.getMeta().addProfile(FhirProfiles.CA_ORGANIZATION);
        organization.setActive(true);
        organization.setName(properties.getOrganizationName());
        return organization;
    }

    static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    static Date toDate(LocalDate date) {
        return toDate(date.atStartOfDay());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
