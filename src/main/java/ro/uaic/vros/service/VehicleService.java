package ro.uaic.vros.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Depot;
import ro.uaic.vros.model.Driver;
import ro.uaic.vros.model.Vehicle;
import ro.uaic.vros.repository.VehicleRepository;

/** LAB4: Enforces vehicle rules and coordinates vehicle/depot/driver repositories. */
@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository repository;
    private final DepotService depots;
    private final DriverService drivers;

    public VehicleService(VehicleRepository repository, DepotService depots, DriverService drivers) {
        this.repository = repository;
        this.depots = depots;
        this.drivers = drivers;
    }

    public List<Vehicle> findAll() { return repository.findAll(); }
    public Optional<Vehicle> findByName(String name) { return repository.findByNameIgnoreCase(name); }
    public Vehicle require(Long id) { return repository.findById(id)
            .orElseThrow(() -> new ro.uaic.vros.web.ResourceNotFoundException("Vehicle", id)); }
    public List<Vehicle> availableAtDepot(Long depotId) {
        return repository.findByAvailableTrueAndDepotId(depotId);
    }
    @Transactional
    public Vehicle create(String name, int capacity, Long depotId, Long driverId) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        Depot depot = depots.require(depotId);
        Driver driver = null;
        if (driverId != null) {
            // LAB4: Friendly early rejection; the UNIQUE database constraint is the final guarantee.
            if (repository.existsByDriverId(driverId)) {
                throw new IllegalArgumentException("Driver already assigned to a vehicle: " + driverId);
            }
            driver = drivers.require(driverId);
        }
        return repository.save(new Vehicle(name, capacity, depot, driver));
    }
    @Transactional
    // LAB4: Bulk JPQL returns updated-row count; zero indicates a missing vehicle.
    public void setAvailability(Long vehicleId, boolean available) {
        if (repository.updateAvailability(vehicleId, available) != 1) {
            throw new IllegalArgumentException("Unknown vehicle: " + vehicleId);
        }
    }
    /**
     * LAB 5 HOMEWORK 2: the REST exercise assumes one depot and no drivers.
     * We reuse the Lab 4 create method, passing null for the optional driver.
     */
    @Transactional
    public Vehicle createDriverless(String name, int capacity, boolean available) {
        var allDepots = depots.findAll();
        if (allDepots.size() != 1) {
            throw new ro.uaic.vros.web.BadRequestException(
                    "REST vehicle creation requires exactly one depot; found " + allDepots.size());
        }
        Vehicle vehicle = create(name, capacity, allDepots.getFirst().getId(), null);
        vehicle.setAvailable(available);
        return repository.save(vehicle);
    }

    /** PUT fully replaces editable fields; no driver can be assigned via REST. */
    @Transactional
    public Vehicle updateDriverless(Long id, String name, int capacity, boolean available) {
        Vehicle vehicle = require(id);
        vehicle.setName(name);
        vehicle.setCapacity(capacity);
        vehicle.setAvailable(available);
        vehicle.setDriver(null);
        return repository.save(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
        // If the vehicle is used by a scenario, the DB returns HTTP 409 via advice.
    }

    public long count() { return repository.count(); }
}
