package ro.uaic.vros.model;

import jakarta.persistence.*;
import java.time.Instant;

/** LAB4: Each delivery belongs to exactly one customer and stores a positive demand. */
@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // LAB4: Owning side of the many-to-one relationship, persisted as customer_id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Column(nullable = false)
    private int quantity;
    @Column(length = 1000)
    private String notes;
    // LAB4: Quote the actual SQL column named timestamp and use PostgreSQL timestamptz.
    @Column(name = "\"timestamp\"", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant registeredAt;

    protected Delivery() {}
    public Delivery(Customer customer, int quantity, String notes, Instant registeredAt) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.customer = customer;
        this.quantity = quantity;
        this.notes = notes;
        this.registeredAt = registeredAt;
    }
    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.quantity = quantity;
    }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getRegisteredAt() { return registeredAt; }
}
