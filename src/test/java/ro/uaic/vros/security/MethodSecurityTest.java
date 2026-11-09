package ro.uaic.vros.security;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ro.uaic.vros.model.Customer;
import ro.uaic.vros.model.Delivery;
import ro.uaic.vros.repository.DeliveryRepository;
import ro.uaic.vros.service.CustomerService;
import ro.uaic.vros.service.DeliveryService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MethodSecurityTest.Config.class)
class MethodSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean DeliveryRepository deliveryRepository() { return mock(DeliveryRepository.class); }
        @Bean CustomerService customerService() { return mock(CustomerService.class); }
        @Bean OwnershipSecurity ownershipSecurity() { return mock(OwnershipSecurity.class); }
        @Bean DeliveryService deliveryService(DeliveryRepository repo, CustomerService customers) {
            return new DeliveryService(repo, customers);
        }
    }
    @Autowired DeliveryService deliveries;
    @Autowired DeliveryRepository repository;
    @Autowired CustomerService customers;
    @Autowired OwnershipSecurity ownership;

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
        reset(repository, customers, ownership);
    }
    private void authenticate(String username, String role) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", authorities));
    }
    @Test
    void unauthenticatedCannotCreateDelivery() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> deliveries.create(1L, 2, "test"));
        verifyNoInteractions(repository);
    }
    @Test
    void customerCannotCreateAnotherCustomersDelivery() {
        authenticate("alice", "CUSTOMER");
        when(ownership.ownsCustomer("alice", 2L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> deliveries.create(2L, 2, "test"));
        verifyNoInteractions(repository);
    }
    @Test
    void customerCanCreateOwnDelivery() {
        authenticate("alice", "CUSTOMER");
        when(ownership.ownsCustomer("alice", 1L)).thenReturn(true);
        when(customers.require(1L)).thenReturn(new Customer("Alice", "alice@example.test",
                "+400000000", "Home", "47,27"));
        when(repository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(deliveries.create(1L, 2, "own delivery"));
        verify(repository).save(any(Delivery.class));
    }
    @Test
    void driverCannotCreateDelivery() {
        authenticate("driver", "DRIVER");
        assertThrows(AccessDeniedException.class, () -> deliveries.create(1L, 2, "test"));
        verifyNoInteractions(repository);
    }
    @Test
    void adminCanCreateDeliveryForAnyone() {
        authenticate("admin", "ADMIN");
        when(customers.require(5L)).thenReturn(new Customer("Bob", "bob@example.test",
                "+400000001", "Work", "47,27"));
        when(repository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        assertNotNull(deliveries.create(5L, 3, "admin delivery"));
        verify(repository).save(any(Delivery.class));
    }
}
