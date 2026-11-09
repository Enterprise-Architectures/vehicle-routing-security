package ro.uaic.vros.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.uaic.vros.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByCustomerId(Long customerId);
    boolean existsByDriverId(Long driverId);
}
