package ro.uaic.vros.web;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ro.uaic.vros.model.Customer;
import ro.uaic.vros.service.CustomerService;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }
    public record CustomerRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank String phone, @NotBlank String address,
                                  @NotBlank String coordinates) {}
    public record CustomerResponse(Long id, String name, String email, String phone,
                                   String address, String coordinates) {
        public static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getEmail(),
                    c.getPhone(), c.getAddress(), c.getCoordinates());
        }
    }
    @GetMapping
    public List<CustomerResponse> all() {
        return service.findAll().stream().map(CustomerResponse::from).toList();
    }
    /**
     * LAB 5 HOMEWORK 4: Existing compulsory endpoint now supports cache
     * validation. Send the returned ETag as If-None-Match to receive HTTP 304.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> one(
            @PathVariable Long id, org.springframework.web.context.request.WebRequest request) {
        CustomerResponse response = CustomerResponse.from(find(id));
        String etag = ETags.weak(response);
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(response);
    }
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer c = service.create(request.name(), request.email(), request.phone(),
                request.address(), request.coordinates());
        return ResponseEntity.created(URI.create("/api/customers/" + c.getId()))
                .body(CustomerResponse.from(c));
    }
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        try {
            return CustomerResponse.from(service.update(id, request.name(), request.email(),
                    request.phone(), request.address(), request.coordinates()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        try { service.delete(id); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
    private Customer find(Long id) {
        try { return service.require(id); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}