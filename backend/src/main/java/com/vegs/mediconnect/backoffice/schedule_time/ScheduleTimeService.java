package com.vegs.mediconnect.backoffice.schedule_time;

import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.datasource.schedule.ScheduleRepository;
import com.vegs.mediconnect.datasource.schedule.ScheduleTime;
import com.vegs.mediconnect.datasource.schedule.ScheduleTimeRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class ScheduleTimeService {

    private final ScheduleTimeRepository scheduleTimeRepository;
    private final ScheduleRepository scheduleRepository;

    public ScheduleTimeService(final ScheduleTimeRepository scheduleTimeRepository, ScheduleRepository scheduleRepository) {
        this.scheduleTimeRepository = scheduleTimeRepository;
        this.scheduleRepository = scheduleRepository;
        ;
    }

    @Transactional
    public List<ScheduleTimeDTO> findAll() {
        final List<ScheduleTime> scheduleTimes = scheduleTimeRepository
                .findAll(Sort.by(
                        "schedule.doctor.lastName",
                        "schedule.doctor.firstName",
                        "schedule.date",
                        "time"));
        return scheduleTimes.stream()
                .map(scheduleTime -> mapToDTO(scheduleTime, new ScheduleTimeDTO()))
                .toList();
    }

    @Transactional
    public ScheduleTimeDTO get(final UUID id) {
        return scheduleTimeRepository.findById(id)
                .map(scheduleTime -> mapToDTO(scheduleTime, new ScheduleTimeDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final ScheduleTimeDTO scheduleTimeDTO) {
        final ScheduleTime scheduleTime = new ScheduleTime();
        mapToEntity(scheduleTimeDTO, scheduleTime);
        return scheduleTimeRepository.save(scheduleTime).getId();
    }

    public void update(final UUID id, final ScheduleTimeDTO scheduleTimeDTO) {
        final ScheduleTime scheduleTime = scheduleTimeRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(scheduleTimeDTO, scheduleTime);
        scheduleTimeRepository.save(scheduleTime);
    }

    public void delete(final UUID id) {
        scheduleTimeRepository.deleteById(id);
    }

    private ScheduleTimeDTO mapToDTO(final ScheduleTime scheduleTime,
            final ScheduleTimeDTO scheduleTimeDTO) {
        scheduleTimeDTO.setId(scheduleTime.getId());
        scheduleTimeDTO.setTime(scheduleTime.getTime());
        scheduleTimeDTO.setScheduleDate(scheduleTime.getSchedule().getDate());
        scheduleTimeDTO.setDoctorName(scheduleTime.getSchedule().getDoctorName());
        return scheduleTimeDTO;
    }

    private void mapToEntity(final ScheduleTimeDTO scheduleTimeDTO,
            final ScheduleTime scheduleTime) {
        scheduleTime.setTime(scheduleTimeDTO.getTime());
        scheduleTime.setSchedule(scheduleRepository.getReferenceById(scheduleTimeDTO.getScheduleId()));
    }

    public boolean isScheduleDateTimeExist(ScheduleDateTimeDTO dateTimeDTO) {
        var schedule = scheduleRepository.getReferenceById(dateTimeDTO.getScheduleId());
        return scheduleTimeRepository
                .findByTimeAndSchedule(dateTimeDTO.getTime(), schedule)
                .isPresent();
    }
}
