package pl.redhat.samples.person.repository;

import org.springframework.data.repository.CrudRepository;
import pl.redhat.samples.person.domain.Person;

import java.util.List;

public interface PersonRepository extends CrudRepository<Person, Long> {
    List<Person> findByLastName(String name);
    List<Person> findByAgeGreaterThan(int age);
}
