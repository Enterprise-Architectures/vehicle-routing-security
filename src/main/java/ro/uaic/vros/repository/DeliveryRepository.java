package ro.uaic.vros.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.uaic.vros.model.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    // LAB4: Derived query navigates the customer relationship through customer.id.
    List<Delivery> findByCustomerId(Long customerId);

    // HOMEWORK: explicitly specified JPQL query.
    // LAB4: Explicit JPQL addresses entity fields (Delivery, registeredAt), not SQL tables.
    // ========================================================================
    // LAB 04 | HOMEWORK | Explicit JPQL query
    // STATEMENT: Implement a repository method with an explicitly written JPQL.
    // IMPLEMENTATION: SELECT d FROM Delivery d uses the Java entity name,
    // d.customer.id traverses the mapped relationship, :customerId and
    // :minimumQuantity are named parameters, and ORDER BY sorts by the mapped
    // registeredAt property (the SQL column is named timestamp).
    // QUIZ: Why does JPQL say Delivery rather than deliveries?
    // ========================================================================
    @Query("select d from Delivery d where d.customer.id = :customerId and d.quantity >= :minimumQuantity order by d.registeredAt desc")
    List<Delivery> findDemandForCustomer(
            @Param("customerId") Long customerId,
            @Param("minimumQuantity") int minimumQuantity);
}
