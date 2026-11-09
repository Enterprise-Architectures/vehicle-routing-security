package ro.uaic.vros;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.uaic.vros.model.Customer;
import ro.uaic.vros.model.Depot;
import ro.uaic.vros.model.Driver;
import ro.uaic.vros.service.*;

/** Explicit opt-in, idempotent database smoke test for the Compulsory portion. */
@Configuration
public class DemoRunnerConfiguration {
    @Bean
    // LAB4: Opt-in startup test: create/find sample rows, exercise services and print counts.
    // ========================================================================
    // LAB 04 | COMPULSORY | Test persistence with CommandLineRunner
    // STATEMENT: Use a CommandLineRunner to test repository operations.
    // IMPLEMENTATION: Spring creates this runner only with vros.demo.enabled=true.
    // After startup it finds or inserts sample depot, driver, customer, vehicle
    // and delivery data, then queries the services and prints entity counts.
    // Existing-record lookups avoid duplicating the demo on repeated runs.
    // QUIZ: When does CommandLineRunner execute relative to Spring startup?
    // ========================================================================
    @ConditionalOnProperty(name = "vros.demo.enabled", havingValue = "true")
    CommandLineRunner demo(DepotService depots, DriverService drivers,
                           CustomerService customers, VehicleService vehicles,
                           DeliveryService deliveries) {
        return args -> {
            Depot depot = depots.findByName("Iasi Central Depot")
                    .orElseGet(() -> depots.create("Iasi Central Depot", "47.1585,27.6014"));
            Driver driver = drivers.findByEmail("driver.demo@vros.example")
                    .orElseGet(() -> drivers.create("Demo Driver", "driver.demo@vros.example", "+40700000000"));
            Customer customer = customers.findByEmail("customer.demo@vros.example")
                    .orElseGet(() -> customers.create("Demo Customer", "customer.demo@vros.example",
                            "+40700000001", "Iasi, Romania", "47.1700,27.5800"));
            vehicles.findByName("VROS Van 1")
                    .orElseGet(() -> vehicles.create("VROS Van 1", 100, depot.getId(), driver.getId()));
            if (deliveries.byCustomer(customer.getId()).isEmpty()) {
                deliveries.create(customer.getId(), 12, "Lab 4 CommandLineRunner smoke test");
            }
            System.out.printf("VROS DEMO OK: depots=%d, drivers=%d, customers=%d, vehicles=%d, deliveries=%d%n",
                    depots.count(), drivers.count(), customers.count(), vehicles.count(), deliveries.count());
            System.out.printf("Available vehicles at depot %d: %d%n",
                    depot.getId(), vehicles.availableAtDepot(depot.getId()).size());
            System.out.printf("Customer %d deliveries with quantity >= 10: %d%n",
                    customer.getId(), deliveries.demandAtLeast(customer.getId(), 10).size());
        };
    }
}
