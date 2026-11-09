package ro.uaic.vros.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.uaic.vros.model.AppUser;
import ro.uaic.vros.service.AccountService;
import ro.uaic.vros.service.JwtAuthService;

@RestController
@Profile("!compulsory")
public class AuthController {
    private final AccountService accounts;
    private final JwtAuthService jwt;
    public AuthController(AccountService accounts, JwtAuthService jwt) {
        this.accounts = accounts; this.jwt = jwt;
    }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {}
    public record RegistrationRequest(
            @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,120}") String username,
            @NotBlank @Size(min=8, max=72) String password,
            @NotBlank String name, @NotBlank @Email String email,
            @NotBlank String phone, @NotBlank String address, @NotBlank String coordinates) {}
    public record RegistrationResponse(Long id, String username, String role, Long customerId) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest input) {
        return new LoginResponse(jwt.login(input.username(), input.password()), "Bearer", 3600);
    }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest input) {
        AppUser user = accounts.registerCustomer(input.username(), input.password(), input.name(),
                input.email(), input.phone(), input.address(), input.coordinates());
        return new RegistrationResponse(user.getId(), user.getUsername(), user.getRole().name(),
                user.getCustomer().getId());
    }
}
