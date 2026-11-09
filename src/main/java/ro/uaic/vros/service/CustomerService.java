package ro.uaic.vros.service;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.uaic.vros.model.Customer;
import ro.uaic.vros.repository.CustomerRepository;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository repository;
    public CustomerService(CustomerRepository repository) { this.repository = repository; }
    public List<Customer> findAll() { return repository.findAll(); }
    public Optional<Customer> findByEmail(String email) { return repository.findByEmailIgnoreCase(email); }
    public Customer require(Long id) { return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown customer: " + id)); }
    @Transactional
    public Customer create(String name, String email, String phone, String address, String coordinates) {
        return repository.save(new Customer(name, email, phone, address, coordinates));
    }
    @Transactional
    public Customer update(Long id, String name, String email, String phone, String address, String coordinates) {
        Customer customer = require(id);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setCoordinates(coordinates);
        return repository.save(customer);
    }
    @Transactional
    public void delete(Long id) {
        Customer customer = require(id);
        repository.delete(customer);
    }
    public long count() { return repository.count(); }
}