package ro.uaic.vros.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.uaic.vros.model.Customer;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // LAB4: Derived query; Spring Data parses the method name and ignores email case.
    Optional<Customer> findByEmailIgnoreCase(String email);
}
