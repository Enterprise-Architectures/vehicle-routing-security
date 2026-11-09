package ro.uaic.vros.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Delivery;
import ro.uaic.vros.model.Scenario;
import ro.uaic.vros.model.Vehicle;
import ro.uaic.vros.repository.DeliveryRepository;
import ro.uaic.vros.repository.ScenarioRepository;
import ro.uaic.vros.repository.VehicleRepository;
import ro.uaic.vros.web.BadRequestException;
import ro.uaic.vros.web.ResourceNotFoundException;

/**
 * LAB 5 HOMEWORK 1 + 2: Scenario CRUD and management of its member resources.
 *
 * This service accepts IDs, resolves them to existing JPA entities, then
 * persists the scenario and *links*, never duplicate vehicle/delivery rows.
 * Transactions keep both join tables consistent if validation fails.
 */
@Service
@Transactional(readOnly = true)
public class ScenarioService {
    private final ScenarioRepository scenarios;
    private final VehicleRepository vehicles;
    private final DeliveryRepository deliveries;

    public ScenarioService(ScenarioRepository scenarios,
                           VehicleRepository vehicles,
                           DeliveryRepository deliveries) {
        this.scenarios = scenarios;
        this.vehicles = vehicles;
        this.deliveries = deliveries;
    }

    public List<Scenario> findAll() { return scenarios.findAll(); }

    public Scenario require(Long id) {
        return scenarios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", id));
    }

    @Transactional
    public Scenario create(String name, Set<Long> vehicleIds, Set<Long> deliveryIds) {
        Scenario scenario = new Scenario(name);
        scenario.replaceMembers(resolveVehicles(vehicleIds), resolveDeliveries(deliveryIds));
        return scenarios.save(scenario);
    }

    /** PUT replaces the complete scenario, including its member sets. */
    @Transactional
    public Scenario update(Long id, String name, Set<Long> vehicleIds, Set<Long> deliveryIds) {
        Scenario scenario = require(id);
        // Resolve *all* referenced IDs before changing a managed entity.
        Set<Vehicle> newVehicles = resolveVehicles(vehicleIds);
        Set<Delivery> newDeliveries = resolveDeliveries(deliveryIds);
        scenario.setName(name);
        scenario.replaceMembers(newVehicles, newDeliveries);
        return scenarios.save(scenario);
    }

    @Transactional
    public void delete(Long id) {
        scenarios.delete(require(id));
        // SQL FK ON DELETE CASCADE clears join links. Member objects survive.
    }

    private Set<Vehicle> resolveVehicles(Set<Long> ids) {
        List<Vehicle> found = vehicles.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new BadRequestException("One or more vehicleIds do not exist: " + ids);
        }
        return new LinkedHashSet<>(found);
    }

    private Set<Delivery> resolveDeliveries(Set<Long> ids) {
        List<Delivery> found = deliveries.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new BadRequestException("One or more deliveryIds do not exist: " + ids);
        }
        return new LinkedHashSet<>(found);
    }
}
