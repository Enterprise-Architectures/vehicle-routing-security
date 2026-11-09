package ro.uaic.vros.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.uaic.vros.model.AppUser;
import ro.uaic.vros.service.AccountService;

@RestController
@Profile("!compulsory")
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AccountService accounts;
    public AdminUserController(AccountService accounts) { this.accounts = accounts; }
    public record DriverAccountRequest(
            @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,120}") String username,
            @NotBlank @Size(min=8, max=72) String password,
            @NotNull @Positive Long driverId) {}
    public record DriverAccountResponse(Long id, String username, String role, Long driverId) {}

    @PostMapping("/drivers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DriverAccountResponse createDriver(@Valid @RequestBody DriverAccountRequest input) {
        AppUser user = accounts.provisionDriver(input.username(), input.password(), input.driverId());
        return new DriverAccountResponse(user.getId(), user.getUsername(),
                user.getRole().name(), user.getDriver().getId());
    }
}
