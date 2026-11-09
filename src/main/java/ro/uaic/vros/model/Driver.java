package ro.uaic.vros.model;

import jakarta.persistence.*;

/** LAB4: A driver reuses contact columns but has an independent drivers table. */
@Entity
@Table(name = "drivers")
public class Driver extends ContactPerson {
    // LAB4: Inverse, optional side; Vehicle.driver owns the unique foreign key.
    @OneToOne(mappedBy = "driver", fetch = FetchType.LAZY)
    private Vehicle vehicle;

    protected Driver() {}
    public Driver(String name, String email, String phone) {
        super(name, email, phone);
    }
    public Vehicle getVehicle() { return vehicle; }
}
