package ro.uaic.vros.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Driver;
import ro.uaic.vros.repository.DriverRepository;

/** LAB4: Encapsulates driver creation and lookups behind a service. */
@Service
@Transactional(readOnly = true)
public class DriverService {
    private final DriverRepository repository;
    public DriverService(DriverRepository repository) { this.repository = repository; }
    public List<Driver> findAll() { return repository.findAll(); }
    public Optional<Driver> findByEmail(String email) { return repository.findByEmailIgnoreCase(email); }
    public Driver require(Long id) { return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown driver: " + id)); }
    // LAB4: This method writes a new Driver row within a transaction.
    @Transactional
    public Driver create(String name, String email, String phone) {
        return repository.save(new Driver(name, email, phone));
    }
    public long count() { return repository.count(); }
}
