package ro.uaic.vros.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Delivery;
import ro.uaic.vros.repository.DeliveryRepository;

/** LAB4: Resolves customer FK and validates demand before persisting deliveries. */
@Service
@Transactional(readOnly = true)
public class DeliveryService {
    private final DeliveryRepository repository;
    private final CustomerService customers;

    public DeliveryService(DeliveryRepository repository, CustomerService customers) {
        this.repository = repository;
        this.customers = customers;
    }
    public List<Delivery> findAll() { return repository.findAll(); }
    public Delivery require(Long id) { return repository.findById(id)
            .orElseThrow(() -> new ro.uaic.vros.web.ResourceNotFoundException("Delivery", id)); }
    public List<Delivery> byCustomer(Long customerId) { return repository.findByCustomerId(customerId); }
    public List<Delivery> demandAtLeast(Long customerId, int minimumQuantity) {
        if (minimumQuantity <= 0) throw new IllegalArgumentException("Minimum quantity must be positive");
        return repository.findDemandForCustomer(customerId, minimumQuantity);
    }
    // LAB4: A write transaction spans customer validation and repository.save().
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and @ownershipSecurity.ownsCustomer(authentication.name, #customerId))")
    public Delivery create(Long customerId, int quantity, String notes) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        return repository.save(new Delivery(customers.require(customerId), quantity, notes, Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
    }
    /**
     * LAB 5 HOMEWORK 2: update mutable fields; preserve server registration time.
     * The delivery may move to another existing customer.
     */
    @Transactional
    public Delivery update(Long id, Long customerId, int quantity, String notes) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Delivery delivery = require(id);
        delivery.setCustomer(customers.require(customerId));
        delivery.setQuantity(quantity);
        delivery.setNotes(notes);
        return repository.save(delivery);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
        // If a scenario references this delivery, FK protection returns 409.
    }

    public long count() { return repository.count(); }
}
