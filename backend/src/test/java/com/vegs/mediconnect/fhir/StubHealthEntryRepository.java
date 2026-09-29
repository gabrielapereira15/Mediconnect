package com.vegs.mediconnect.fhir;

import com.vegs.mediconnect.datasource.health.HealthEntry;
import com.vegs.mediconnect.datasource.health.HealthEntryRepository;
import com.vegs.mediconnect.datasource.health.HealthEntryType;
import com.vegs.mediconnect.datasource.patient.Patient;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * A fixed list of entries standing in for the repository.
 *
 * The summary tests are about the shape of the document, not about JPA, so
 * they run without a Spring context. Only the two finder methods
 * PatientSummaryService actually calls do anything; the rest of the
 * JpaRepository surface is here because the interface demands it.
 */
class StubHealthEntryRepository implements HealthEntryRepository {

    private final List<HealthEntry> entries;

    StubHealthEntryRepository(List<HealthEntry> entries) {
        this.entries = entries;
    }

    @Override
    public List<HealthEntry> findAllByPatientOrderByDateCreatedAsc(Patient patient) {
        return entries;
    }

    @Override
    public List<HealthEntry> findAllByPatientAndType(Patient patient, HealthEntryType type) {
        return entries.stream().filter(e -> e.getType() == type).toList();
    }

    // ---- unused JpaRepository surface -----------------------------------

    @Override public void flush() { }
    @Override public <S extends HealthEntry> S saveAndFlush(S entity) { throw unsupported(); }
    @Override public <S extends HealthEntry> List<S> saveAllAndFlush(Iterable<S> entities) { throw unsupported(); }
    @Override public void deleteAllInBatch(Iterable<HealthEntry> entities) { throw unsupported(); }
    @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { throw unsupported(); }
    @Override public void deleteAllInBatch() { throw unsupported(); }
    @Override public HealthEntry getOne(UUID id) { throw unsupported(); }
    @Override public HealthEntry getById(UUID id) { throw unsupported(); }
    @Override public HealthEntry getReferenceById(UUID id) { throw unsupported(); }
    @Override public <S extends HealthEntry> List<S> findAll(Example<S> example) { throw unsupported(); }
    @Override public <S extends HealthEntry> List<S> findAll(Example<S> example, Sort sort) { throw unsupported(); }
    @Override public <S extends HealthEntry> List<S> saveAll(Iterable<S> entities) { throw unsupported(); }
    @Override public List<HealthEntry> findAll() { return entries; }
    @Override public List<HealthEntry> findAllById(Iterable<UUID> ids) { throw unsupported(); }
    @Override public <S extends HealthEntry> S save(S entity) { throw unsupported(); }
    @Override public Optional<HealthEntry> findById(UUID id) { throw unsupported(); }
    @Override public boolean existsById(UUID id) { throw unsupported(); }
    @Override public long count() { return entries.size(); }
    @Override public void deleteById(UUID id) { throw unsupported(); }
    @Override public void delete(HealthEntry entity) { throw unsupported(); }
    @Override public void deleteAllById(Iterable<? extends UUID> ids) { throw unsupported(); }
    @Override public void deleteAll(Iterable<? extends HealthEntry> entities) { throw unsupported(); }
    @Override public void deleteAll() { throw unsupported(); }
    @Override public List<HealthEntry> findAll(Sort sort) { throw unsupported(); }
    @Override public Page<HealthEntry> findAll(Pageable pageable) { throw unsupported(); }
    @Override public <S extends HealthEntry> Optional<S> findOne(Example<S> example) { throw unsupported(); }
    @Override public <S extends HealthEntry> Page<S> findAll(Example<S> example, Pageable pageable) { throw unsupported(); }
    @Override public <S extends HealthEntry> long count(Example<S> example) { throw unsupported(); }
    @Override public <S extends HealthEntry> boolean exists(Example<S> example) { throw unsupported(); }
    @Override public <S extends HealthEntry, R> R findBy(Example<S> example,
            Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("not needed by the summary tests");
    }
}
