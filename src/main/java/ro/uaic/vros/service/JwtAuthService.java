package ro.uaic.vros.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.uaic.vros.repository.AppUserRepository;

@Service
@Profile("!compulsory")
public class JwtAuthService {
    private final AuthenticationManager manager;
    private final JwtEncoder encoder;
    private final AppUserRepository users;
    public JwtAuthService(AuthenticationManager manager, JwtEncoder encoder, AppUserRepository users) {
        this.manager = manager; this.encoder = encoder; this.users = users;
    }
    public String login(String username, String password) {
        String canonical = AccountService.canonicalUsername(username);
        try { manager.authenticate(new UsernamePasswordAuthenticationToken(canonical, password)); }
        catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        var user = users.findByUsername(canonical).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("urn:vros").subject(user.getUsername())
                .issuedAt(now).expiresAt(now.plus(Duration.ofHours(1)))
                .claim("roles", List.of(user.getRole().name())).build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
