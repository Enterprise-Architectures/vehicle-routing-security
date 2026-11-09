package ro.uaic.vros.service;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import ro.uaic.vros.model.*;

class JpaMappingsTest {
    private static Field field(Class<?> type, String name) throws Exception {
        return type.getDeclaredField(name);
    }

    @Test
    void allFiveDomainTypesAreJpaEntities() {
        for (Class<?> type : new Class<?>[]{Depot.class, Driver.class, Customer.class, Vehicle.class, Delivery.class}) {
            assertTrue(type.isAnnotationPresent(Entity.class), type.getSimpleName());
        }
        assertTrue(ContactPerson.class.isAnnotationPresent(MappedSuperclass.class));
    }

    @Test
    void associationsCoverOneToOneOneToManyAndManyToOne() throws Exception {
        assertTrue(field(Vehicle.class, "driver").isAnnotationPresent(OneToOne.class));
        assertTrue(field(Driver.class, "vehicle").isAnnotationPresent(OneToOne.class));
        assertTrue(field(Depot.class, "vehicles").isAnnotationPresent(OneToMany.class));
        assertTrue(field(Customer.class, "deliveries").isAnnotationPresent(OneToMany.class));
        assertTrue(field(Vehicle.class, "depot").isAnnotationPresent(ManyToOne.class));
        assertTrue(field(Delivery.class, "customer").isAnnotationPresent(ManyToOne.class));
    }

    @Test
    void contactPersonProvidesGeneratedIdentifierForBothSubtypes() throws Exception {
        assertTrue(field(ContactPerson.class, "id").isAnnotationPresent(Id.class));
        assertEquals(GenerationType.IDENTITY, field(ContactPerson.class, "id")
                .getAnnotation(GeneratedValue.class).strategy());
        assertTrue(ContactPerson.class.isAssignableFrom(Driver.class));
        assertTrue(ContactPerson.class.isAssignableFrom(Customer.class));
    }
}
