package com.example.optimeal_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Minimal Spring Security configuration.
 *
 * <p>Spring Security is on the classpath (required by spring-boot-starter-security)
 * but we do not use its principal/authentication infrastructure.
 * This configuration:
 * <ul>
 *   <li>Disables CSRF (stateless REST API — no session cookies).</li>
 *   <li>Disables form login and HTTP Basic.</li>
 *   <li>Marks the session policy as STATELESS so no {@code HttpSession} is created.</li>
 *   <li>Permits all requests at the Spring Security level — actual authentication
 *       is enforced by {@link FirebaseTokenFilter} which runs before the security chain
 *       and short-circuits with 401 before the request ever reaches a controller.</li>
 *   <li>Registers {@link FirebaseTokenFilter} before Spring Security's own
 *       {@link UsernamePasswordAuthenticationFilter}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;

    public SecurityConfig(FirebaseTokenFilter firebaseTokenFilter) {
        this.firebaseTokenFilter = firebaseTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless REST API — no CSRF tokens needed.
            .csrf(AbstractHttpConfigurer::disable)

            // No form login, no HTTP Basic challenge.
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // Never create or use an HttpSession — Firebase token is re-verified per request.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // All route-level access decisions are delegated to FirebaseTokenFilter.
            // Spring Security itself permits everything; the filter handles 401.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            // Register the Firebase filter early in the chain, before Spring Security
            // attempts any form-based or Basic authentication processing.
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
