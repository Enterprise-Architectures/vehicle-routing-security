package ro.uaic.vros.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Shared persistence mapping, not a polymorphic entity:
 * drivers and customers have independent tables/identities.
 */
@MappedSuperclass
public abstract class ContactPerson {
    // LAB4: Mapped-superclass fields are copied into both concrete entity mappings;
    // there is no separate ContactPerson table or shared person identity.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 32)
    private String phone;

    protected ContactPerson() {}

    protected ContactPerson(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
