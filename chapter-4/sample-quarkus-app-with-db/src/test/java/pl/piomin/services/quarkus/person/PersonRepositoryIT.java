package pl.piomin.services.quarkus.person;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.piomin.services.quarkus.person.model.Gender;
import pl.piomin.services.quarkus.person.model.Person;
import pl.piomin.services.quarkus.person.repository.PersonRepository;

import jakarta.inject.Inject;

@QuarkusTest
@TestTransaction
public class PersonRepositoryIT {

    @Inject
    PersonRepository personRepository;

    @Test
    void addPerson() {
        Person newPerson = new Person();
        newPerson.age = 22;
        newPerson.name = "TestNew";
        newPerson.gender = Gender.FEMALE;
        personRepository.persist(newPerson);
        Assertions.assertNotNull(newPerson.id);
    }
}
