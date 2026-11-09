package ro.uaic.vros.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.uaic.vros.model.Driver;
public interface DriverRepository extends JpaRepository<Driver, Long> {
    // LAB4: Derived lookup used by the idempotent CommandLineRunner demo.
    Optional<Driver> findByEmailIgnoreCase(String email);
}
