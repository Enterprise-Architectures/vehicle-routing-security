package ro.uaic.vros.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.*;
import ro.uaic.vros.repository.*;

/**
 * Opt-in PostgreSQL integration tests:
 * PowerShell: $env:RUN_PG_INTEGRATION_TESTS='true'; mvn test
 */
@SpringBootTest(properties = "vros.demo.enabled=false")
@EnabledIfEnvironmentVariable(named = "RUN_PG_INTEGRATION_TESTS", matches = "true")
@Transactional
class PostgresRepositoryIntegrationTest {
    @Autowired DepotRepository depots;
    @Autowired DriverRepository drivers;
    @Autowired VehicleRepository vehicles;
    @Autowired CustomerRepository customers;
    @Autowired DeliveryRepository deliveries;

    @Test
    void derivedAndTransactionalModifyingQueriesExecuteOnPostgresql() {
        String suffix = Long.toString(System.nanoTime());
        Depot depot = depots.save(new Depot("Depot-" + suffix, "47.1585,27.6014"));
        Driver driver = drivers.save(new Driver("Tester", "driver-" + suffix + "@example.test", "0123"));
        Vehicle vehicle = vehicles.saveAndFlush(new Vehicle("Van-" + suffix, 20, depot, driver));
        Long depotId = depot.getId();
        Long vehicleId = vehicle.getId();

        assertTrue(vehicles.findByAvailableTrueAndDepotId(depotId)
                .stream().anyMatch(v -> v.getId().equals(vehicleId)));
        assertEquals(1, vehicles.updateAvailability(vehicleId, false));
        assertFalse(vehicles.findById(vehicleId).orElseThrow().isAvailable());
        assertTrue(vehicles.findByAvailableTrueAndDepotId(depotId).isEmpty());
        assertEquals(1, vehicles.updateAvailability(vehicleId, true));
        assertTrue(vehicles.findById(vehicleId).orElseThrow().isAvailable());
    }

    @Test
    void explicitJpqlFiltersDemandCorrectly() {
        String suffix = Long.toString(System.nanoTime());
        Customer customer = customers.save(new Customer("Tester", "customer-" + suffix + "@example.test",
                "0123", "Iasi", "47.17,27.58"));
        deliveries.save(new Delivery(customer, 2, "small", Instant.now()));
        deliveries.save(new Delivery(customer, 12, "large", Instant.now()));

        var filtered = deliveries.findDemandForCustomer(customer.getId(), 10);
        assertEquals(1, filtered.size());
        assertEquals(12, filtered.getFirst().getQuantity());
    }
}
