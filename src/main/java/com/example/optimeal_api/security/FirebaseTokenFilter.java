package com.example.optimeal_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Servlet filter that authenticates every API request using Firebase ID tokens.
 *
 * On successful verification, the Firebase UID is stored as a request attribute.
 * On failure, the filter short-circuits with a 401 Unauthorized JSON response.
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger       log           = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private static final String       BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper MAPPER        = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Preflight OPTIONS requests carry no Authorization header. Bypass them.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            rejectUnauthorized(response, "Missing or malformed Authorization header. " +
                    "Expected: 'Authorization: Bearer <firebase-id-token>'");
            return;
        }

        String idToken = authHeader.substring(BEARER_PREFIX.length()).strip();

        try {
            FirebaseToken verifiedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            // Store as a request attribute so controllers cannot be bypassed by
            // client-supplied UID values in the request body or query string.
            String uid = verifiedToken.getUid();
            request.setAttribute("firebaseUid", uid);

            log.debug("Firebase token verified for uid={}", uid);
            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException ex) {
            log.warn("Firebase token verification failed: {}", ex.getMessage());
            rejectUnauthorized(response, "Firebase token invalid or expired: " + ex.getMessage());
        }
    }

    private void rejectUnauthorized(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status",    HttpStatus.UNAUTHORIZED.value(),
                "error",     HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "message",   message
        );

        response.getWriter().write(MAPPER.writeValueAsString(body));
    }
}
