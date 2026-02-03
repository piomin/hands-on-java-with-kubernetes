package pl.piomin.ch4;

import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.piomin.ch4.domain.Person;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureRestTestClient
public class PersonControllerIT {

    @Autowired
    RestTestClient restTestClient;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withExposedPorts(5432);

    @Test
    @Order(1)
    void add() {
        Person person = Instancio.of(Person.class)
                .ignore(Select.field("id"))
                .create();
        person = restTestClient.post().uri("/persons")
                .body(person)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Person.class)
                .returnResult().getResponseBody();
        assertNotNull(person);
        assertNotNull(person.getId());
    }

    @Test
    @Order(2)
    void updateAndGet() {
        final Integer id = 1;
        Person person = Instancio.of(Person.class)
                .set(Select.field("id"), id)
                .create();
        restTestClient.put().uri("/persons")
                .body(person)
                .exchange()
                .expectStatus().isOk();
        Person updated = restTestClient.get().uri("/persons/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Person.class)
                .returnResult().getResponseBody();
        assertNotNull(updated);
        assertNotNull(updated.getId());
        assertEquals(id, updated.getId());
    }

    @Test
    @Order(3)
    void getAll() {
        Person[] persons = restTestClient.get().uri("/persons")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Person[].class)
                .returnResult().getResponseBody();
        assertNotNull(persons);
        assertEquals(1, persons.length);
    }

    @Test
    @Order(4)
    void deleteAndGet() {
        restTestClient.delete().uri("/persons/{id}", 1)
                .exchange()
                .expectStatus().isOk();
        Person person = restTestClient.get().uri("/persons/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Person.class)
                .returnResult().getResponseBody();
        assertNull(person);
    }

}
