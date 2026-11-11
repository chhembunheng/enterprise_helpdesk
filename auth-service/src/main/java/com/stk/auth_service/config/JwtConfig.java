package com.stk.auth_service.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfig {
    @Bean
    SecretKey jwtKey(@Value("${helpdesk.jwt.secret}") String encoded) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT_SECRET must be base64", ex); }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET needs at least 32 random bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${helpdesk.jwt.issuer}") String issuer,
                          @Value("${helpdesk.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        OAuth2TokenValidator<Jwt> identityValidator = jwt -> {
            try { java.util.UUID.fromString(jwt.getSubject());
                if (jwt.getExpiresAt() != null) return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException | NullPointerException ex) { /* reject invalid/missing subject */ }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid identity or expiry", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, identityValidator));
        return decoder;
    }
}
