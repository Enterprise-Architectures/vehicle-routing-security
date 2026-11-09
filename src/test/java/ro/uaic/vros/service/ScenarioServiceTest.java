package ro.uaic.vros.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.uaic.vros.model.*;
import ro.uaic.vros.repository.*;
import ro.uaic.vros.web.BadRequestException;
import ro.uaic.vros.web.ResourceNotFoundException;

/**
 * LAB 5 HOMEWORK: Business-rule tests independent of PostgreSQL.
 *
 * Mocked repositories let us explain precisely what the service is responsible
 * for: resolving member IDs, changing associations, and reporting absent data.
 * The separate HTTP smoke script checks persistence and response formatting.
 */
@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {
    @Mock ScenarioRepository scenarios;
    @Mock VehicleRepository vehicles;
    @Mock DeliveryRepository deliveries;
    ScenarioService service;

    @BeforeEach
    void setup() {
        service = new ScenarioService(scenarios, vehicles, deliveries);
    }

    @Test
    void creationLinksExistingObjectsWithoutCreatingCopies() {
        Depot depot = new Depot("Only depot", "47.17,27.58");
        Vehicle van = new Vehicle("Van", 100, depot, null);
        Customer customer = new Customer("C", "c@example.test", "07", "Iasi", "47.17,27.58");
        Delivery parcel = new Delivery(customer, 10, "Books", java.time.Instant.now());
        when(vehicles.findAllById(Set.of(3L))).thenReturn(List.of(van));
        when(deliveries.findAllById(Set.of(5L))).thenReturn(List.of(parcel));
        when(scenarios.save(any(Scenario.class))).thenAnswer(call -> call.getArgument(0));

        Scenario scenario = service.create("Morning", Set.of(3L), Set.of(5L));

        assertEquals("Morning", scenario.getName());
        assertEquals(Set.of(van), scenario.getVehicles());
        assertEquals(Set.of(parcel), scenario.getDeliveries());
        verify(scenarios).save(scenario);
    }

    @Test
    void creationWithUnknownVehicleIdIsRejectedAndNotSaved() {
        when(vehicles.findAllById(Set.of(999L))).thenReturn(List.of());
        assertThrows(BadRequestException.class,
                () -> service.create("Broken", Set.of(999L), Set.of()));
        verify(scenarios, never()).save(any());
        verifyNoInteractions(deliveries);
    }

    @Test
    void updateReplacesBothMemberSetsRatherThanAppending() {
        Scenario scenario = new Scenario("Old");
        scenario.getVehicles().add(new Vehicle("Old van", 5,
                new Depot("Depot", "47.1,27.5"), null));
        when(scenarios.findById(7L)).thenReturn(Optional.of(scenario));
        when(scenarios.save(scenario)).thenReturn(scenario);

        Scenario result = service.update(7L, "New", Set.of(), Set.of());

        assertEquals("New", result.getName());
        assertTrue(result.getVehicles().isEmpty());
        assertTrue(result.getDeliveries().isEmpty());
    }

    @Test
    void missingScenarioUsesCustomRuntimeException() {
        when(scenarios.findById(31L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.require(31L));
    }

    @Test
    void deletingScenarioDoesNotDeleteVehiclesOrDeliveries() {
        Scenario scenario = new Scenario("Morning");
        when(scenarios.findById(10L)).thenReturn(Optional.of(scenario));

        service.delete(10L);

        verify(scenarios).delete(scenario);
        verifyNoInteractions(vehicles, deliveries);
    }
}
