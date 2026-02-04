package pl.piomin.ch9;

import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.piomin.ch9.domain.Person;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureRestTestClient
public class PersonControllerIntegrationTests {

    private static final String API_PATH = "/persons";

    @Autowired
    RestTestClient restTestClient;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.7")
            .withExposedPorts(5432);

    @Test
    @Order(1)
    void add() {
        restTestClient.post().uri(API_PATH).body(Instancio.create(Person.class))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Person.class)
                .value(person -> assertNotNull(person.getId()));
    }

    @Test
    @Order(2)
    void updateAndGet() {
        final Integer id = 1;
        Person person = Instancio.of(Person.class)
                .set(Select.field("id"), id)
                .create();
        restTestClient.put().uri(API_PATH)
                .body(person)
                .exchange()
                .expectStatus().is2xxSuccessful();
        restTestClient.get().uri(API_PATH + "/{id}", 1L)
                .exchange()
                .expectBody(Person.class)
                .value(personUpdated -> assertEquals("Updated", personUpdated.getFirstName()));
    }

    @Test
    @Order(3)
    void getAll() {
        restTestClient.get().uri(API_PATH)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Person[].class)
                .value(persons -> assertTrue(persons.length > 0));
    }

    @Test
    @Order(4)
    void deleteAndGet() {
        restTestClient.delete().uri(API_PATH + "/{id}", 1L)
                .exchange()
                .expectStatus().is2xxSuccessful();
        restTestClient.get().uri(API_PATH + "/{id}", 1L)
                .exchange()
                .expectStatus().is5xxServerError();
    }

}
