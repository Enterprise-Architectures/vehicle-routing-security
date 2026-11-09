package ro.uaic.vros.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LAB 5 HOMEWORK 1: A scenario is the *input* to a future routing solver.
 *
 * Many scenarios can reuse the same vehicle or delivery, and each scenario can
 * include many vehicles/deliveries. These are genuine many-to-many relations
 * represented by join tables, not CSV columns or serialized lists.
 *
 * No CascadeType.REMOVE: deleting a scenario must never delete its vehicles or
 * customer deliveries. We only delete the scenario and its link rows.
 */
@Entity
@Table(name = "scenarios")
public class Scenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    // Owning side: the join rows reference both scenario and vehicle IDs.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "scenario_vehicles",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "vehicle_id"))
    private Set<Vehicle> vehicles = new LinkedHashSet<>();

    // A separate many-to-many association for deliveries.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "scenario_deliveries",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "delivery_id"))
    private Set<Delivery> deliveries = new LinkedHashSet<>();

    protected Scenario() {} // JPA needs a no-argument constructor.

    public Scenario(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Vehicle> getVehicles() { return vehicles; }
    public Set<Delivery> getDeliveries() { return deliveries; }

    /** Replace membership, rather than appending indefinitely on each PUT. */
    public void replaceMembers(Set<Vehicle> vehicles, Set<Delivery> deliveries) {
        this.vehicles.clear();
        this.vehicles.addAll(vehicles);
        this.deliveries.clear();
        this.deliveries.addAll(deliveries);
    }
}
