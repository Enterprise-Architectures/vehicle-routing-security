package ro.uaic.vros.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.uaic.vros.model.Depot;
import ro.uaic.vros.model.Driver;
import ro.uaic.vros.model.Vehicle;
import ro.uaic.vros.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {
    @Mock VehicleRepository repository;
    @Mock DepotService depots;
    @Mock DriverService drivers;
    VehicleService service;

    @BeforeEach
    void setup() { service = new VehicleService(repository, depots, drivers); }

    @Test
    void createsVehicleAtDepotWithAvailableFlagAndDriver() {
        Depot depot = new Depot("Depot", "47.1,27.6");
        Driver driver = new Driver("Driver", "d@example.org", "0101");
        when(depots.require(5L)).thenReturn(depot);
        when(drivers.require(7L)).thenReturn(driver);
        when(repository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle vehicle = service.create("Truck", 50, 5L, 7L);
        assertEquals(50, vehicle.getCapacity());
        assertSame(depot, vehicle.getDepot());
        assertSame(driver, vehicle.getDriver());
        assertTrue(vehicle.isAvailable());
        verify(repository).existsByDriverId(7L);
    }

    @Test
    void rejectsDriverAlreadyAssigned() {
        when(depots.require(5L)).thenReturn(new Depot("Depot", "47.1,27.6"));
        when(repository.existsByDriverId(7L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.create("Truck", 50, 5L, 7L));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create("Truck", 0, 1L, null));
        verifyNoInteractions(repository, depots, drivers);
    }

    @Test
    void usesDerivedAvailableVehiclesQuery() {
        when(repository.findByAvailableTrueAndDepotId(3L)).thenReturn(List.of());
        assertTrue(service.availableAtDepot(3L).isEmpty());
        verify(repository).findByAvailableTrueAndDepotId(3L);
    }

    @Test
    void modifyingQueryUpdatesExactlyOneVehicle() {
        when(repository.updateAvailability(12L, false)).thenReturn(1);
        assertDoesNotThrow(() -> service.setAvailability(12L, false));
        verify(repository).updateAvailability(12L, false);
    }

    @Test
    void modifyingQueryRejectsMissingVehicle() {
        when(repository.updateAvailability(13L, false)).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(13L, false));
    }
}
