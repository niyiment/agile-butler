package com.niyiment.agilebutler.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path}")
    private Resource credentialsResource;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        if (!credentialsResource.exists()) {
            log.warn("Firebase credentials not found at {}. Push notifications will be disabled.",
                    credentialsResource.getDescription());
            return noOpFirebaseMessaging();
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsResource.getInputStream());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp app;
        try {
            app = FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            app = FirebaseApp.initializeApp(options);
        }

        log.info("Firebase initialized successfully");
        return FirebaseMessaging.getInstance(app);
    }


    private FirebaseMessaging noOpFirebaseMessaging() {
        return null;
    }
}