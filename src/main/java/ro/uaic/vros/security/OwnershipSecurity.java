package ro.uaic.vros.security;

import org.springframework.stereotype.Component;
import ro.uaic.vros.repository.AppUserRepository;

@Component("ownershipSecurity")
public class OwnershipSecurity {
    private final AppUserRepository users;
    public OwnershipSecurity(AppUserRepository users) { this.users = users; }
    public boolean ownsCustomer(String username, Long customerId) {
        if (username == null || customerId == null) return false;
        return users.findByUsername(username)
                .map(u -> u.getCustomer() != null && customerId.equals(u.getCustomer().getId()))
                .orElse(false);
    }
}
