package ro.uaic.vros.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/** LAB4: Customer rows are mapped to the customers table; contact fields come from ContactPerson. */
@Entity
@Table(name = "customers")
public class Customer extends ContactPerson {
    @Column(nullable = false, length = 255)
    private String address;
    @Column(nullable = false, length = 64)
    private String coordinates;
    // LAB4: Inverse side of Delivery.customer; 'mappedBy' prevents an extra join table.
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private Set<Delivery> deliveries = new HashSet<>();

    protected Customer() {}
    public Customer(String name, String email, String phone, String address, String coordinates) {
        super(name, email, phone);
        this.address = address;
        this.coordinates = coordinates;
    }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCoordinates() { return coordinates; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public Set<Delivery> getDeliveries() { return deliveries; }
}
