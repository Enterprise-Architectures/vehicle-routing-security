package ro.uaic.vros.service;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.uaic.vros.model.*;
import ro.uaic.vros.repository.*;

@Service
public class AccountService {
    private final AppUserRepository users;
    private final CustomerRepository customers;
    private final DriverRepository drivers;
    private final PasswordEncoder encoder;
    public AccountService(AppUserRepository users, CustomerRepository customers,
                          DriverRepository drivers, PasswordEncoder encoder) {
        this.users = users; this.customers = customers; this.drivers = drivers; this.encoder = encoder;
    }
    public static String canonicalUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
    @Transactional
    public AppUser registerCustomer(String username, String password, String name, String email,
                                    String phone, String address, String coordinates) {
        String canonical = canonicalUsername(username);
        if (users.existsByUsername(canonical)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        Customer customer = customers.save(new Customer(name, email, phone, address, coordinates));
        return users.save(new AppUser(canonical, encoder.encode(password), Role.CUSTOMER, customer, null));
    }
    @Transactional
    public AppUser provisionDriver(String username, String password, Long driverId) {
        String canonical = canonicalUsername(username);
        if (users.existsByUsername(canonical)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (users.existsByDriverId(driverId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver already has an account");
        }
        Driver driver = drivers.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        return users.save(new AppUser(canonical, encoder.encode(password), Role.DRIVER, null, driver));
    }
}
