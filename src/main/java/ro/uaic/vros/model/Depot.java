package ro.uaic.vros.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/** LAB4: A depot is a vehicle's home location; routing itself is deferred to later labs. */
@Entity
@Table(name = "depots")
public class Depot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120, unique = true)
    private String name;
    /** WGS84 "latitude,longitude" string for this lab; spatial type can follow in a later lab. */
    @Column(nullable = false, length = 64)
    private String coordinates;
    // LAB4: One depot can be referenced by arbitrarily many vehicles.
    // LAB 04 | HOMEWORK | STATEMENT: Map Depot to its vehicles.
    // IMPLEMENTATION: @OneToMany is the inverse collection; mappedBy refers
    // to Vehicle.depot, which owns the depot_id foreign key.
    // QUIZ: Why is mappedBy necessary in a bidirectional relationship?
    @OneToMany(mappedBy = "depot", fetch = FetchType.LAZY)
    private Set<Vehicle> vehicles = new HashSet<>();

    protected Depot() {}
    public Depot(String name, String coordinates) {
        this.name = name;
        this.coordinates = coordinates;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoordinates() { return coordinates; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public Set<Vehicle> getVehicles() { return vehicles; }
}
