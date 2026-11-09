package ro.uaic.vros.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.uaic.vros.model.Customer;
import ro.uaic.vros.model.Delivery;
import ro.uaic.vros.repository.DeliveryRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    @Mock DeliveryRepository repository;
    @Mock CustomerService customers;

    @Test
    void createSetsCustomerQuantityAndTimestamp() {
        var service = new DeliveryService(repository, customers);
        Customer customer = new Customer("C", "c@example.org", "012", "Iasi", "47,27");
        when(customers.require(2L)).thenReturn(customer);
        when(repository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        Delivery result = service.create(2L, 3, "notes");
        assertEquals(3, result.getQuantity());
        assertSame(customer, result.getCustomer());
        assertNotNull(result.getRegisteredAt());
    }

    @Test
    void usesExplicitJpqlRepositoryMethod() {
        var service = new DeliveryService(repository, customers);
        when(repository.findDemandForCustomer(2L, 4)).thenReturn(List.of());
        assertTrue(service.demandAtLeast(2L, 4).isEmpty());
        verify(repository).findDemandForCustomer(2L, 4);
    }

    @Test
    void rejectsInvalidQuantities() {
        var service = new DeliveryService(repository, customers);
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> service.demandAtLeast(1L, 0));
        verifyNoInteractions(repository, customers);
    }
}
