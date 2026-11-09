package ro.uaic.vros.web;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("compulsory")
public class MockLoginController {
    @PostMapping("/login")
    public Map<String, String> mockLogin() {
        return Map.of("message", "Mock login endpoint (compulsory profile only)");
    }
}
