package pl.piomin.services.callme;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "VERSION=v1")
@AutoConfigureRestTestClient
public class CallmeControllerTests {

    @Autowired
    RestTestClient restTestClient;

    @Test
    void ping() {
        restTestClient.get().uri("/callme/ping")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .value(Assertions::assertNotNull)
                .value(s -> assertEquals("I'm callme-service localhost:v1", s));
    }

    @Test
    void pingWithRandomDelay() {
        restTestClient.get().uri("/callme/ping-with-random-delay")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .value(Assertions::assertNotNull)
                .value(s -> assertEquals("I'm callme-service v1", s));
    }
}
