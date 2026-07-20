package com.solaria.auth.security.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfig {
    @Bean(destroyMethod = "delete")
    @ConditionalOnProperty(prefix = "app.security.firebase", name = ["enabled"], havingValue = "true")
    fun firebaseApp(properties: FirebaseProperties): FirebaseApp {
        require(properties.projectId.isNotBlank()) { "FIREBASE_PROJECT_ID is required when Firebase is enabled" }
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(properties.projectId)
            .build()
        return FirebaseApp.initializeApp(options, APP_NAME)
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.firebase", name = ["enabled"], havingValue = "true")
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)

    private companion object {
        const val APP_NAME = "solaria-auth"
    }
}
