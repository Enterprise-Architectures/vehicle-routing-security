package ro.uaic.vros.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.uaic.vros.model.Scenario;

/**
 * EntityGraph loads both member sets while the query runs. With
 * spring.jpa.open-in-view=false, controllers must not depend on
 * lazy loading after the service transaction has ended.
 */
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    @Override
    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    List<Scenario> findAll();

    @Override
    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    Optional<Scenario> findById(Long id);
}
