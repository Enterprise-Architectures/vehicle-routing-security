package ro.uaic.vros.model;

import jakarta.persistence.*;

/** LAB4: Each vehicle is tied to a required depot and an optional, unique driver. */
@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120, unique = true)
    private String name;
    @Column(nullable = false)
    private int capacity;
    // LAB4: Many vehicles can share one depot; depot_id is the owning foreign key.
    // ========================================================================
    // LAB 04 | HOMEWORK | Entity relationship: Vehicle -> Depot (many-to-one)
    // STATEMENT: Model the relationship between vehicles and depots using JPA.
    // IMPLEMENTATION: @ManyToOne means several Vehicle rows can reference one
    // Depot. @JoinColumn maps this property to vehicles.depot_id; nullable=false
    // and optional=false require a depot for each vehicle.
    // QUIZ: Which side owns the foreign key? Vehicle.depot.
    // ========================================================================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    @Column(nullable = false)
    private boolean available = true;
    // Unique FK enforces one driver per vehicle AND at most one vehicle per driver.
    // LAB4: Optional driver_id is UNIQUE, so two vehicles cannot share a driver.
    // LAB 04 | HOMEWORK | STATEMENT: Map Vehicle to an optional Driver.
    // IMPLEMENTATION: @OneToOne and UNIQUE driver_id enforce at most one
    // vehicle per driver. This Vehicle field owns the foreign key.
    // QUIZ: A foreign key alone does not enforce uniqueness.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;

    protected Vehicle() {}
    public Vehicle(String name, int capacity, Depot depot, Driver driver) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.name = name;
        this.capacity = capacity;
        this.depot = depot;
        this.driver = driver;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }
    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
}
