package com.example.WorkHub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ObservabilityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealth_returnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void livenessAndReadiness_areAvailable() {
        ResponseEntity<String> liveness = restTemplate.getForEntity("/actuator/health/liveness", String.class);
        assertEquals(HttpStatus.OK, liveness.getStatusCode());

        ResponseEntity<String> readiness = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        assertEquals(HttpStatus.OK, readiness.getStatusCode());
    }
}
