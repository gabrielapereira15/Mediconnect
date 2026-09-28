package com.vegs.mediconnect.backoffice.doctor;

import com.vegs.mediconnect.backoffice.util.NotFoundException;
import com.vegs.mediconnect.backoffice.util.ReferencedWarning;
import com.vegs.mediconnect.datasource.doctor.Doctor;
import com.vegs.mediconnect.datasource.doctor.DoctorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public List<DoctorDTO> findAll() {
        final List<Doctor> doctors = doctorRepository
                .findAll(Sort.by("lastName", "firstName"));
        return doctors.stream()
                .map(doctor -> mapToDTO(doctor, new DoctorDTO()))
                .toList();
    }

    public DoctorDTO get(final UUID id) {
        return doctorRepository.findById(id)
                .map(doctor -> mapToDTO(doctor, new DoctorDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final DoctorDTO doctorDTO) {
        final Doctor doctor = new Doctor();
        mapToEntity(doctorDTO, doctor);
        doctor.setProfilePhoto(getBytes(doctorDTO.getImage()));
        doctor.setProfilePhotoExtension(doctorDTO.getImage().getContentType());
        return doctorRepository.save(doctor).getId();
    }

    public void update(final UUID id, final DoctorDTO doctorDTO) {
        final Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(doctorDTO, doctor);
        doctorRepository.save(doctor);
    }

    public void delete(final UUID id) {
        doctorRepository.deleteById(id);
    }

    private DoctorDTO mapToDTO(final Doctor doctor, final DoctorDTO doctorDTO) {
        doctorDTO.setId(doctor.getId());
        doctorDTO.setFirstName(doctor.getFirstName());
        doctorDTO.setLastName(doctor.getLastName());
        doctorDTO.setExperienceInYears(doctor.getExperienceInYears());
        doctorDTO.setAbout(doctor.getAbout());
        doctorDTO.setSpecialty(doctor.getSpecialty());
        return doctorDTO;
    }

    private Doctor mapToEntity(final DoctorDTO doctorDTO, final Doctor doctor) {
        doctor.setFirstName(doctorDTO.getFirstName());
        doctor.setLastName(doctorDTO.getLastName());
        doctor.setExperienceInYears(doctorDTO.getExperienceInYears());
        doctor.setAbout(doctorDTO.getAbout());
        doctor.setSpecialty(doctorDTO.getSpecialty());
        return doctor;
    }

    private byte[] getBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            log.error("Error to get bytes from doctor.image", e);
        }
        return null;
    }

    public ReferencedWarning getReferencedWarning(final UUID id) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        return null;
    }

    public void updatePhoto(UUID id, @Valid MultipartFile newPhoto) {
        final Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        doctor.setProfilePhoto(getBytes(newPhoto));
        doctor.setProfilePhotoExtension(newPhoto.getContentType());
        doctorRepository.save(doctor);
    }
}
