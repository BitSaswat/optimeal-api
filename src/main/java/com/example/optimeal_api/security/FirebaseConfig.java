package com.example.optimeal_api.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises the Firebase Admin SDK once at application startup.
 *
 * <p>Credentials are loaded via {@link ClassPathResource} rather than a raw
 * {@code FileInputStream} so that the path resolution is classpath-relative and
 * portable across local, JAR, and containerised deployments.
 *
 * <p>{@code service-account.json} must be placed in {@code src/main/resources/}
 * and must be excluded from version control.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initFirebase() throws IOException {
        // Guard against double-initialisation in test contexts or when another
        // auto-configuration has already registered a default FirebaseApp.
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialised — skipping.");
            return;
        }

        ClassPathResource credentialResource = new ClassPathResource("service-account.json");
        try (InputStream credentialStream = credentialResource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialStream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialised successfully.");
        }
    }
}
