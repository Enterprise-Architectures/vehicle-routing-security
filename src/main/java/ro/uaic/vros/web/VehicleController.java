package ro.uaic.vros.web;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.uaic.vros.model.Vehicle;
import ro.uaic.vros.service.VehicleService;

/**
 * LAB 5 HOMEWORK 2 + 3: Vehicle CRUD, separate input/output DTOs, Bean Validation.
 *
 * This API assumes a single depot. Requests never contain a driver ID. We
 * expose depotId as read-only information, but not an editable field here.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) { this.service = service; }

    /** @Valid enforces these annotations *before* the service is invoked. */
    public record VehicleRequest(
            @NotBlank @Size(max = 120) String name,
            @Positive int capacity,
            @NotNull Boolean available) {}

    /** Sending a DTO avoids exposing lazy JPA associations or infinite recursion. */
    public record VehicleResponse(Long id, String name, int capacity,
                                  boolean available, Long depotId) {
        public static VehicleResponse from(Vehicle vehicle) {
            return new VehicleResponse(vehicle.getId(), vehicle.getName(),
                    vehicle.getCapacity(), vehicle.isAvailable(), vehicle.getDepot().getId());
        }
    }

    @GetMapping
    public List<VehicleResponse> all() {
        return service.findAll().stream().map(VehicleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VehicleResponse one(@PathVariable Long id) {
        return VehicleResponse.from(service.require(id));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = service.createDriverless(
                request.name(), request.capacity(), request.available());
        return ResponseEntity.created(URI.create("/api/vehicles/" + vehicle.getId()))
                .body(VehicleResponse.from(vehicle));
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable Long id,
                                  @Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(service.updateDriverless(
                id, request.name(), request.capacity(), request.available()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
