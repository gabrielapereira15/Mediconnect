package com.vegs.mediconnect.backoffice.waitlist;

import com.vegs.mediconnect.backoffice.appointment.ClinicAppointmentService;
import com.vegs.mediconnect.backoffice.shared.Display;
import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.appointment.Appointment;
import com.vegs.mediconnect.datasource.appointment.AppointmentRepository;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import com.vegs.mediconnect.datasource.patient.PatientRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntry;
import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import com.vegs.mediconnect.mobile.waitlist.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The waitlist as the desk sees it (board B05): who is waiting for what,
 * which freed slots are being held for someone, and which have nobody who
 * can use them.
 */
@Service
@RequiredArgsConstructor
public class WaitlistBoard {

    /** How far ahead a freed slot is worth putting on a card. */
    static final int FREED_DAYS = 7;

    private final WaitlistEntryRepository waitlistRepository;
    private final WaitlistService waitlistService;
    private final ScheduleTimeRepository scheduleTimeRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public enum Tab {
        WAITING, OFFERED, BOOKED, WITHDRAWN;

        public String getKey() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public String getLabel() {
            return name().charAt(0) + name().substring(1).toLowerCase(Locale.ENGLISH);
        }

        public static Tab of(String key) {
            for (Tab tab : values()) {
                if (tab.getKey().equalsIgnoreCase(key)) {
                    return tab;
                }
            }
            return WAITING;
        }
    }

    /** A freed slot worth the desk's attention. */
    public record Card(UUID slotId, String when, String doctor, String line, boolean held,
                       String actionLabel, String actionLink) {
    }

    public record Row(UUID id, String patient, String initials, String avatarClass, String contact,
                      String doctor, String currentVisit, String canComeFrom, String status,
                      String statusClass, String lastOffer, boolean canOffer, boolean canEndOffer,
                      boolean canWithdraw) {
    }

    public record Board(List<Card> cards, Map<Tab, Integer> counts, List<Row> rows) {

        public int count(Tab tab) {
            return counts.getOrDefault(tab, 0);
        }
    }

    @Transactional(readOnly = true)
    public Board board(Tab tab) {
        LocalDateTime now = LocalDateTime.now();
        List<WaitlistEntry> all = waitlistRepository.findAllByOrderByDateCreatedDesc();

        Map<Tab, Integer> counts = new EnumMap<>(Tab.class);
        for (Tab each : Tab.values()) {
            counts.put(each, 0);
        }
        for (WaitlistEntry entry : all) {
            counts.merge(Tab.valueOf(entry.getStatus().name()), 1, Integer::sum);
        }

        List<Row> rows = all.stream()
                .filter(entry -> entry.getStatus().name().equals(tab.name()))
                .sorted(tab == Tab.WAITING || tab == Tab.OFFERED
                        ? Comparator.comparing(WaitlistEntry::getDateCreated)
                        : Comparator.comparing(WaitlistEntry::getLastUpdated).reversed())
                .map(entry -> row(entry, now))
                .toList();

        return new Board(cards(all, now), counts, rows);
    }

    private List<Card> cards(List<WaitlistEntry> all, LocalDateTime now) {
        List<Card> cards = new ArrayList<>();
        for (WaitlistEntry entry : all) {
            if (entry.getStatus() != WaitlistStatus.OFFERED || entry.getOfferedSlot() == null) {
                continue;
            }
            ScheduleTime slot = entry.getOfferedSlot();
            cards.add(new Card(slot.getId(), Display.dayAndTime(slot.getDateTime()),
                    "Dr. " + slot.getSchedule().getDoctor().getLastName(),
                    "Held for " + Display.name(entry.getPatient().getFirstName(), entry.getPatient().getLastName())
                            + " · offer ends " + Display.time(Display.local(entry.getOfferExpiresAt()).toLocalTime()),
                    true, "Manage",
                    "/schedule?date=" + slot.getSchedule().getDate() + "&slot=" + slot.getId()));
        }

        // Freed and still free: cancelled bookings whose slot nobody has
        // taken since, in the week ahead.
        LocalDate today = now.toLocalDate();
        Set<UUID> seen = new java.util.HashSet<>();
        for (Appointment appointment : appointmentRepository.findAllBetween(today, today.plusDays(FREED_DAYS))) {
            ScheduleTime slot = appointment.getScheduleTime();
            if (!Boolean.TRUE.equals(appointment.getCanceled())
                    || !Boolean.TRUE.equals(slot.getAvailable())
                    || Boolean.TRUE.equals(slot.getBlocked())
                    || !slot.getDateTime().isAfter(now)
                    || !seen.add(slot.getId())) {
                continue;
            }
            Doctor doctor = slot.getSchedule().getDoctor();
            long fit = waitlistRepository.findAllByDoctorAndStatusOrderByDateCreatedAsc(doctor, WaitlistStatus.WAITING)
                    .stream()
                    .filter(entry -> entry.wants(slot.getSchedule().getDate()))
                    .count();
            cards.add(new Card(slot.getId(), Display.dayAndTime(slot.getDateTime()), "Dr. " + doctor.getLastName(),
                    fit == 0 ? "Freed · nobody on Dr. " + doctor.getLastName() + "'s waitlist fits"
                            : "Freed · " + fit + " waiting could take it",
                    false, "Book someone", "/appointments/new?slot=" + slot.getId()));
        }
        return cards.stream().limit(4).toList();
    }

    private Row row(WaitlistEntry entry, LocalDateTime now) {
        var patient = entry.getPatient();
        String name = Display.name(patient.getFirstName(), patient.getLastName());
        // Paused in the app: still on the list, but not to be offered anything.
        boolean paused = entry.getStatus() == WaitlistStatus.WAITING && !WaitlistService.offersOn(patient);
        String status = paused
                ? "Offers paused"
                : entry.getStatus().name().charAt(0) + entry.getStatus().name().substring(1).toLowerCase(Locale.ENGLISH);
        String statusClass = paused ? "" : switch (entry.getStatus()) {
            case OFFERED -> "mc-badge-sun";
            case BOOKED -> "mc-badge-success";
            case WITHDRAWN -> "";
            default -> "mc-badge-info";
        };
        String lastOffer = entry.getLastOfferedAt() == null ? "—" : ago(entry.getLastOfferedAt())
                + (entry.getStatus() == WaitlistStatus.WAITING && entry.getPassedSlotId() != null ? " · passed" : "");
        boolean open = entry.getStatus() == WaitlistStatus.WAITING || entry.getStatus() == WaitlistStatus.OFFERED;
        return new Row(entry.getId(), name, Display.initials(name), Display.avatarClass(name),
                patient.getPhoneNumber() != null ? patient.getPhoneNumber() : patient.getEmail(),
                "Dr. " + entry.getDoctor().getLastName(),
                Display.day(entry.getCurrentAppointmentDate()),
                entry.getAvailableFrom() == null || !entry.getAvailableFrom().isAfter(now.toLocalDate())
                        ? "Now" : Display.day(entry.getAvailableFrom()),
                status, statusClass, lastOffer,
                entry.getStatus() == WaitlistStatus.WAITING && !paused,
                entry.getStatus() == WaitlistStatus.OFFERED,
                open);
    }

    static String ago(OffsetDateTime at) {
        long minutes = Duration.between(at, OffsetDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        if (minutes < 60 * 24) {
            return (minutes / 60) + " h ago";
        }
        long days = minutes / (60 * 24);
        return days == 1 ? "yesterday" : days + " days ago";
    }

    // ---- offering one slot ------------------------------------------------------------

    /** The person, and the earlier free times that could be offered to them. */
    public record OfferOptions(UUID entryId, String patient, String doctor, String currentVisit,
                               Map<String, List<ClinicAppointmentService.SlotChoice>> slotsByDay) {
    }

    @Transactional(readOnly = true)
    public OfferOptions offerOptions(UUID entryId) {
        WaitlistEntry entry = waitlistRepository.findById(entryId).orElseThrow(NotFoundException::new);
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, List<ClinicAppointmentService.SlotChoice>> byDay = new LinkedHashMap<>();
        // Up to the last day they want, which is the day they asked for
        // itself when they hold no visit, so the picker lists every time
        // offerTo would accept.
        scheduleTimeRepository.findAllBetween(LocalDate.now(), entry.lastWantedDate()).stream()
                .filter(slot -> slot.getSchedule().getDoctor().getId().equals(entry.getDoctor().getId()))
                .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()) && !Boolean.TRUE.equals(slot.getBlocked()))
                .filter(slot -> entry.getAvailableFrom() == null
                        || !slot.getSchedule().getDate().isBefore(entry.getAvailableFrom()))
                .filter(slot -> WaitlistService.holdEnds(slot, now) != null)
                .sorted(Comparator.comparing(ScheduleTime::getDateTime))
                .forEach(slot -> byDay.computeIfAbsent(Display.day(slot.getSchedule().getDate()),
                        key -> new ArrayList<>()).add(new ClinicAppointmentService.SlotChoice(
                        slot.getId(), Display.time(slot.getTime()))));
        return new OfferOptions(entry.getId(),
                Display.name(entry.getPatient().getFirstName(), entry.getPatient().getLastName()),
                "Dr. " + entry.getDoctor().getLastName(),
                Display.day(entry.getCurrentAppointmentDate()),
                byDay);
    }

    /** @return the problem, or null once the slot is held for them */
    @Transactional
    public String offer(UUID entryId, UUID slotId) {
        WaitlistEntry entry = waitlistRepository.findById(entryId).orElseThrow(NotFoundException::new);
        ScheduleTime slot = scheduleTimeRepository.findById(slotId).orElseThrow(NotFoundException::new);
        return waitlistService.offerTo(entry, slot);
    }

    /** Ends a hold early; the slot goes to the next person or back on sale. */
    @Transactional
    public void endOffer(UUID entryId) {
        WaitlistEntry entry = waitlistRepository.findById(entryId).orElseThrow(NotFoundException::new);
        if (entry.getStatus() == WaitlistStatus.OFFERED) {
            waitlistService.release(entry, WaitlistStatus.WAITING);
        }
    }

    /** Takes someone off the list, passing on anything held for them. Nothing is deleted. */
    @Transactional
    public void withdraw(UUID entryId) {
        WaitlistEntry entry = waitlistRepository.findById(entryId).orElseThrow(NotFoundException::new);
        if (entry.getStatus() == WaitlistStatus.OFFERED) {
            waitlistService.release(entry, WaitlistStatus.WITHDRAWN);
        } else if (entry.getStatus() == WaitlistStatus.WAITING) {
            entry.setStatus(WaitlistStatus.WITHDRAWN);
            waitlistRepository.save(entry);
        }
    }

    // ---- adding someone ---------------------------------------------------------------------

    /** Patients with an upcoming visit, each with the visits they could be waiting to improve on. */
    public record Candidate(UUID appointmentId, String label) {
    }

    @Transactional(readOnly = true)
    public List<Candidate> candidates() {
        LocalDateTime now = LocalDateTime.now();
        return appointmentRepository.findAll().stream()
                .filter(appointment -> !Boolean.TRUE.equals(appointment.getCanceled()))
                .filter(appointment -> appointment.getDateTime().isAfter(now.plusDays(1)))
                .sorted(Comparator.comparing((Appointment appointment) -> appointment.getPatient().getLastName())
                        .thenComparing(Appointment::getDateTime))
                .map(appointment -> new Candidate(appointment.getId(),
                        Display.name(appointment.getPatient().getFirstName(), appointment.getPatient().getLastName())
                                + " — Dr. " + appointment.getDoctor().getLastName()
                                + ", " + Display.dayAndTime(appointment.getDateTime())))
                .collect(Collectors.toList());
    }

    /**
     * Puts a patient on the list for an earlier time than a visit they hold.
     *
     * @return the problem, or null once added
     */
    @Transactional
    public String add(UUID appointmentId, LocalDate availableFrom) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(NotFoundException::new);
        var patient = appointment.getPatient();
        var doctor = appointment.getDoctor();
        if (waitlistRepository.existsByPatientAndDoctorAndStatus(patient, doctor, WaitlistStatus.WAITING)
                || waitlistRepository.existsByPatientAndDoctorAndStatus(patient, doctor, WaitlistStatus.OFFERED)) {
            return "They are already on Dr. " + doctor.getLastName() + "'s waitlist.";
        }
        var entry = new WaitlistEntry();
        entry.setPatient(patientRepository.getReferenceById(patient.getId()));
        entry.setDoctor(doctorRepository.getReferenceById(doctor.getId()));
        entry.setCurrentAppointmentDate(appointment.getDateTime().toLocalDate());
        entry.setHoldsVisit(true);
        entry.setAvailableFrom(availableFrom);
        entry.setStatus(WaitlistStatus.WAITING);
        waitlistRepository.save(entry);
        return null;
    }
}
