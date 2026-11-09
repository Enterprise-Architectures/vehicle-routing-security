package ro.uaic.vros.security;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@Profile("!compulsory")
public class JwtConfiguration {
    @Bean
    SecretKey jwtSecretKey(@Value("${vros.jwt.secret-base64:}") String secret) {
        byte[] raw;
        try { raw = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("VROS_JWT_SECRET_BASE64 must be valid Base64", e); }
        if (raw.length < 32) {
            throw new IllegalStateException("Set VROS_JWT_SECRET_BASE64 to at least 32 random bytes (Base64 encoded)");
        }
        return new SecretKeySpec(raw, "HmacSHA256");
    }
    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }
    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("urn:vros"));
        return decoder;
    }
}
