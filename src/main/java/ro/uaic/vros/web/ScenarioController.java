package ro.uaic.vros.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import ro.uaic.vros.model.Scenario;
import ro.uaic.vros.service.ScenarioService;

/**
 * LAB 5 HOMEWORK 1-6: Scenario endpoints exercise relationships, DTOs, validation,
 * conditional GET, JSON/XML content negotiation, and OpenAPI documentation.
 */
@RestController
@RequestMapping("/api/scenarios")
@Tag(name = "Scenarios", description = "Named vehicle-and-delivery sets for future routing optimization")
public class ScenarioController {
    private final ScenarioService service;

    public ScenarioController(ScenarioService service) { this.service = service; }

    /**
     * Request refers to existing entities by their IDs, NOT nested JPA objects.
     * @NotNull rejects missing collections; [] represents a valid empty set.
     * Container element @Positive validates *each* referenced ID.
     */
    public record ScenarioRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull Set<@NotNull @Positive Long> vehicleIds,
            @NotNull Set<@NotNull @Positive Long> deliveryIds) {}

    /** Sort IDs so response and ETag do not depend on database iteration order. */
    public record ScenarioResponse(Long id, String name,
                                   List<Long> vehicleIds, List<Long> deliveryIds) {
        public static ScenarioResponse from(Scenario scenario) {
            return new ScenarioResponse(scenario.getId(), scenario.getName(),
                    scenario.getVehicles().stream().map(v -> v.getId()).sorted().toList(),
                    scenario.getDeliveries().stream().map(d -> d.getId()).sorted().toList());
        }
    }

    @GetMapping
    @Operation(summary = "List routing scenarios", description = "Returns scenarios and their member IDs.")
    public List<ScenarioResponse> all() {
        return service.findAll().stream().map(ScenarioResponse::from).toList();
    }

    /**
     * LAB 5 HOMEWORK 4-5:
     * - HTTP Accept determines JSON vs XML via registered Jackson converters.
     * - WebRequest.checkNotModified implements If-None-Match, including weak
     *   comparison and comma-separated tag lists. A match -> 304, empty body.
     * - Vary: Accept tells caches that representations use content negotiation.
     */
    @GetMapping(value = "/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Get a scenario in JSON or XML (supports If-None-Match)",
            description = "Use Accept: application/json or application/xml. Repeat ETag in If-None-Match to receive 304.")
    @ApiResponse(responseCode = "200", description = "Scenario representation")
    @ApiResponse(responseCode = "304", description = "Unchanged; no response body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Scenario not found")
    public ResponseEntity<ScenarioResponse> one(@PathVariable Long id, WebRequest request) {
        ScenarioResponse response = ScenarioResponse.from(service.require(id));
        String etag = ETags.weak(response);
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag).varyBy("Accept").build();
        }
        return ResponseEntity.ok().eTag(etag).varyBy("Accept").body(response);
    }

    @PostMapping
    @Operation(summary = "Create a scenario", description = "All vehicleIds and deliveryIds must already exist.")
    @ApiResponse(responseCode = "201", description = "Scenario created")
    @ApiResponse(responseCode = "400", description = "Invalid request or nonexistent member IDs")
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody ScenarioRequest request) {
        Scenario scenario = service.create(
                request.name(), request.vehicleIds(), request.deliveryIds());
        return ResponseEntity.created(URI.create("/api/scenarios/" + scenario.getId()))
                .body(ScenarioResponse.from(scenario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace scenario", description = "PUT replaces the name and *both* member sets.")
    public ScenarioResponse update(@PathVariable Long id,
                                   @Valid @RequestBody ScenarioRequest request) {
        return ScenarioResponse.from(service.update(
                id, request.name(), request.vehicleIds(), request.deliveryIds()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a scenario", description = "Deletes links, never the vehicles or deliveries.")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
