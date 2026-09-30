package com.vegs.mediconnect.backoffice.patient;

import ca.uhn.fhir.context.FhirContext;
import com.vegs.mediconnect.backoffice.appointment.AppointmentsView;
import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryType;
import com.vegs.mediconnect.datasource.patient.Patient;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.previsit.PreVisitForm;
import com.vegs.mediconnect.datasource.previsit.PreVisitFormRepository;
import com.vegs.mediconnect.datasource.review.ReviewRepository;
import com.vegs.mediconnect.fhir.PatientSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Puts a patient's chart together for a clinician (board B04), and hands
 * over the same PS-CA summary the patient can share from the app.
 */
@Service
@RequiredArgsConstructor
public class PatientChartService {

    /** The app's symptom codes in the words the patient ticked. */
    static final Map<String, String> SYMPTOMS = Map.of(
            "fever", "Fever",
            "cough", "Cough",
            "chest_pain", "Chest pain",
            "fatigue", "Fatigue",
            "breathlessness", "Shortness of breath",
            "headache", "Headache",
            "none", "None of these");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final HealthEntryRepository healthEntryRepository;
    private final PreVisitFormRepository formRepository;
    private final ReviewRepository reviewRepository;
    private final PatientSummaryService summaryService;
    private final FhirContext fhirContext;

    /** One line of the patients list. */
    public record Line(UUID id, String name, String initials, String avatarClass, String birthdate,
                       String phone, String email, int allergyCount, String nextVisit) {
    }

    @Transactional(readOnly = true)
    public List<Line> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        LocalDateTime now = LocalDateTime.now();
        return patientRepository.findAll(Sort.by("lastName", "firstName")).stream()
                .filter(patient -> needle.isEmpty() || haystack(patient).contains(needle))
                .map(patient -> {
                    String name = Display.name(patient.getFirstName(), patient.getLastName());
                    int allergies = (int) healthEntryRepository
                            .findAllByPatientAndType(patient, HealthEntryType.ALLERGY).stream()
                            .filter(HealthEntry::isActive).count();
                    String next = appointmentRepository.findAllByPatient(patient).stream()
                            .filter(PatientChartService::active)
                            .filter(appointment -> appointment.getDateTime().isAfter(now))
                            .min(Comparator.comparing(Appointment::getDateTime))
                            .map(appointment -> Display.dayAndTime(appointment.getDateTime())
                                    + " · Dr. " + appointment.getDoctor().getLastName())
                            .orElse(null);
                    return new Line(patient.getId(), name, Display.initials(name), Display.avatarClass(name),
                            patient.getBirthdate(), patient.getPhoneNumber(), patient.getEmail(), allergies, next);
                })
                .toList();
    }

    private static String haystack(Patient patient) {
        return String.join(" ", Arrays.asList(patient.getFirstName(), patient.getLastName(),
                patient.getEmail(), patient.getBirthdate(), patient.getPhoneNumber())).toLowerCase(Locale.ENGLISH);
    }

    @Transactional(readOnly = true)
    public PatientChart chart(UUID id) {
        Patient patient = patientRepository.findById(id).orElseThrow(NotFoundException::new);
        String name = Display.name(patient.getFirstName(), patient.getLastName());
        LocalDateTime now = LocalDateTime.now();

        List<HealthEntry> record = healthEntryRepository.findAllByPatientOrderByDateCreatedAsc(patient);
        List<PatientChart.Allergy> allergies = record.stream()
                .filter(entry -> entry.getType() == HealthEntryType.ALLERGY)
                .filter(HealthEntry::isActive)
                .map(entry -> new PatientChart.Allergy(entry.getDescription(), entry.getNote()))
                .toList();
        List<PatientChart.Entry> entries = record.stream()
                .sorted(Comparator.comparing((HealthEntry entry) -> entry.getType().ordinal())
                        .thenComparing(entry -> !entry.isActive()))
                .map(PatientChartService::entry)
                .toList();

        List<Appointment> visits = appointmentRepository.findAllByPatient(patient).stream()
                .filter(PatientChartService::listed)
                .toList();
        List<PatientChart.Visit> upcoming = visits.stream()
                .filter(PatientChartService::active)
                .filter(appointment -> appointment.getDateTime().isAfter(now))
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .map(appointment -> visit(appointment, now))
                .toList();
        List<PatientChart.Visit> past = visits.stream()
                .filter(appointment -> !appointment.getDateTime().isAfter(now)
                        || Boolean.TRUE.equals(appointment.getCanceled()))
                .sorted(Comparator.comparing(Appointment::getDateTime).reversed())
                .map(appointment -> visit(appointment, now))
                .toList();

        List<PatientChart.Form> forms = visits.stream()
                .filter(appointment -> appointment.getFormSubmittedAt() != null)
                .sorted(Comparator.comparing(Appointment::getFormSubmittedAt).reversed())
                .map(appointment -> formRepository.findByAppointment(appointment)
                        .map(form -> form(appointment, form))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PatientChart(patient.getId(), name, Display.initials(name), Display.avatarClass(name),
                demographics(patient), patient.getPhoneNumber(), patient.getEmail(), patient.getAddress(),
                healthCard(patient), allergies, entries, upcoming, past, forms, visits.size());
    }

    /**
     * The PS-CA document for this patient, as FHIR JSON. The same one the
     * patient can share from the app, so the clinic and the patient never
     * hold two versions of the summary.
     */
    @Transactional(readOnly = true)
    public String summaryDocument(UUID id) {
        Patient patient = patientRepository.findById(id).orElseThrow(NotFoundException::new);
        return fhirContext.newJsonParser().setPrettyPrint(true)
                .encodeResourceToString(summaryService.buildSummary(patient));
    }

    // ---- pieces ---------------------------------------------------------------------

    private static String demographics(Patient patient) {
        List<String> parts = new ArrayList<>();
        if (patient.getGender() != null && !patient.getGender().isBlank()) {
            parts.add(patient.getGender());
        }
        if (patient.getBirthdate() != null) {
            try {
                parts.add(String.valueOf(Period.between(LocalDate.parse(patient.getBirthdate()), LocalDate.now()).getYears()));
            } catch (DateTimeParseException ignored) {
                // An unreadable date is shown as it was entered, below.
            }
            parts.add(patient.getBirthdate());
        }
        return String.join(" · ", parts);
    }

    /** "Health card ••• 7890 · ON": the last four, as it is read back at a desk. */
    private static String healthCard(Patient patient) {
        String number = patient.getHealthCardNumber();
        if (number == null || number.isBlank()) {
            return null;
        }
        String digits = number.replaceAll("\\s", "");
        String last4 = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
        return "Health card ••• " + last4
                + (patient.getHealthCardProvince() == null ? "" : " · " + patient.getHealthCardProvince());
    }

    private static PatientChart.Entry entry(HealthEntry entry) {
        String label;
        String badge;
        switch (entry.getType()) {
            case ALLERGY -> { label = "Allergy"; badge = "mc-badge-danger"; }
            case MEDICATION -> { label = "Medication"; badge = "mc-badge-brand"; }
            default -> { label = "Condition"; badge = "mc-badge-info"; }
        }
        boolean coded = entry.getCode() != null && !entry.getCode().isBlank();
        String coding = coded ? codeSystemName(entry.getCodeSystem()) : "Free text";
        return new PatientChart.Entry(label, badge, entry.getDescription(), entry.getNote(),
                entry.getOnsetDate() == null ? "—" : entry.getOnsetDate().toString(),
                coding, coded, entry.isActive());
    }

    private static String codeSystemName(String system) {
        if (system == null) {
            return "Coded";
        }
        if (system.contains("snomed")) {
            return "SNOMED CT";
        }
        if (system.contains("rxnorm")) {
            return "RxNorm";
        }
        if (system.contains("icd")) {
            return "ICD";
        }
        return "Coded";
    }

    private PatientChart.Visit visit(Appointment appointment, LocalDateTime now) {
        var doctor = appointment.getDoctor();
        LocalDate day = appointment.getDateTime().toLocalDate();
        var status = AppointmentsView.statusOf(appointment, now);

        List<String> sub = new ArrayList<>();
        sub.add(doctor.getSpecialty());
        if (appointment.getBookedForName() != null) {
            sub.add("For " + appointment.getBookedForName());
        }
        if (appointment.getDateTime().isBefore(now)) {
            reviewRepository.findByAppointment(appointment).ifPresent(review ->
                    sub.add("reviewed " + Math.round(review.getScore()) + " ★"));
        }
        return new PatientChart.Visit(
                appointment.getId(),
                day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                String.valueOf(day.getDayOfMonth()),
                Display.time(appointment.getDateTime().toLocalTime()) + " · Dr. " + doctor.getLastName(),
                sub.stream().filter(java.util.Objects::nonNull).collect(Collectors.joining(" · ")),
                status.getLabel(),
                status.getBadgeClass(),
                "/appointments?tab=" + AppointmentsView.tabOf(appointment, now.toLocalDate()).getKey()
                        + "&selected=" + appointment.getId());
    }

    private static PatientChart.Form form(Appointment appointment, PreVisitForm form) {
        List<PatientChart.Answer> answers = new ArrayList<>();
        answers.add(new PatientChart.Answer("Reason for visit", orNotAnswered(form.getReason())));
        answers.add(new PatientChart.Answer("Recent symptoms", symptoms(form.getSymptoms())));
        answers.add(new PatientChart.Answer("Surgery in the last year", yesNo(form.getHadSurgery())));
        answers.add(new PatientChart.Answer("Smokes", yesNo(form.getSmokes())));
        answers.add(new PatientChart.Answer("Drinks alcohol", yesNo(form.getDrinksAlcohol())));
        if (form.getNotes() != null && !form.getNotes().isBlank()) {
            answers.add(new PatientChart.Answer("Anything else", form.getNotes()));
        }
        String heading = "Dr. " + appointment.getDoctor().getLastName() + " · "
                + Display.day(appointment.getDateTime().toLocalDate());
        return new PatientChart.Form(heading, answers);
    }

    static String symptoms(String stored) {
        if (stored == null || stored.isBlank()) {
            return "Not answered";
        }
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> SYMPTOMS.getOrDefault(code, code))
                .collect(Collectors.joining(", "));
    }

    static String yesNo(String answer) {
        if (answer == null) {
            return "Not answered";
        }
        return switch (answer) {
            case "YES" -> "Yes";
            case "NO" -> "No";
            case "UNSURE" -> "Prefers not to say";
            default -> answer;
        };
    }

    private static String orNotAnswered(String value) {
        return value == null || value.isBlank() ? "Not answered" : value;
    }

    private static boolean listed(Appointment appointment) {
        return AppointmentsView.listed(appointment);
    }

    private static boolean active(Appointment appointment) {
        return !Boolean.TRUE.equals(appointment.getCanceled()) && AppointmentsView.listed(appointment);
    }
}
