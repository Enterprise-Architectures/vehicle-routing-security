package ro.uaic.vros.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // HOMEWORK: Spring Data derived query.
    // LAB4: Spring Data derives a SELECT for available vehicles belonging to this depot.
    // ========================================================================
    // LAB 04 | HOMEWORK | Derived query method
    // STATEMENT: Implement a query derived from a Spring Data repository name.
    // IMPLEMENTATION: Spring parses findBy + AvailableTrue + And + DepotId,
    // generating a SELECT for available vehicles whose depot ID matches the
    // argument. No @Query or handwritten SQL is necessary.
    // QUIZ: What changes if AvailableTrue becomes AvailableFalse?
    // ========================================================================
    List<Vehicle> findByAvailableTrueAndDepotId(Long depotId);
    Optional<Vehicle> findByNameIgnoreCase(String name);
    boolean existsByDriverId(Long driverId);

    // HOMEWORK: Transactional modifying JPQL query (bulk update).
    // LAB4: This JPQL UPDATE requires a transaction. Flush/clear avoid stale managed entities.
    // ========================================================================
    // LAB 04 | HOMEWORK | Transactional modifying query
    // STATEMENT: Implement a modifying JPQL query using a transaction.
    // IMPLEMENTATION: @Query supplies JPQL UPDATE on the Vehicle entity;
    // @Modifying tells Spring Data this changes rows rather than selecting them;
    // @Transactional provides a write transaction. The int result is the number
    // of updated rows. flushAutomatically synchronizes pending changes before
    // execution; clearAutomatically clears managed entities afterward.
    // QUIZ: Why are @Modifying and @Transactional both necessary here?
    // ========================================================================
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Vehicle v set v.available = :available where v.id = :vehicleId")
    int updateAvailability(@Param("vehicleId") Long vehicleId, @Param("available") boolean available);
}
