package ro.uaic.vros.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.uaic.vros.model.AppUser;
import ro.uaic.vros.model.Role;
import ro.uaic.vros.repository.AppUserRepository;
import ro.uaic.vros.service.AccountService;

@Configuration
@Profile("!compulsory")
public class AdminBootstrap {
    @Bean
    ApplicationRunner bootstrapAdmin(AppUserRepository users, PasswordEncoder encoder,
            @Value("${vros.admin.username:}") String username,
            @Value("${vros.admin.password:}") String password) {
        return args -> {
            if (username.isBlank() || password.isBlank()) return;
            if (password.length() < 12) {
                throw new IllegalStateException("VROS_ADMIN_PASSWORD must have >=12 characters");
            }
            String canonical = AccountService.canonicalUsername(username);
            var existing = users.findByUsername(canonical);
            if (existing.isPresent()) {
                if (existing.get().getRole() != Role.ADMIN)
                    throw new IllegalStateException("Admin username belongs to a non-admin account");
                return;
            }
            users.save(new AppUser(canonical, encoder.encode(password), Role.ADMIN, null, null));
        };
    }
}
