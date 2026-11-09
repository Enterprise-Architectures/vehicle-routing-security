package ro.uaic.vros.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Depot;
import ro.uaic.vros.repository.DepotRepository;

/** LAB4: Uses repository methods to create depots and read depot data. */
@Service
@Transactional(readOnly = true)
public class DepotService {
    private final DepotRepository repository;
    public DepotService(DepotRepository repository) { this.repository = repository; }
    public List<Depot> findAll() { return repository.findAll(); }
    public Optional<Depot> findByName(String name) { return repository.findByNameIgnoreCase(name); }
    public Depot require(Long id) { return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown depot: " + id)); }
    // LAB4: Writes require a non-read-only transaction.
    @Transactional
    public Depot create(String name, String coordinates) {
        return repository.save(new Depot(name, coordinates));
    }
    public long count() { return repository.count(); }
}
