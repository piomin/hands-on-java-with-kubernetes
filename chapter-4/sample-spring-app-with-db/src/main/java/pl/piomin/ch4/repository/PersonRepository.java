package pl.piomin.ch4.repository;

import org.springframework.data.repository.CrudRepository;
import pl.piomin.ch4.domain.Person;

import java.util.List;

public interface PersonRepository extends CrudRepository<Person, Long> {
    List<Person> findByLastName(String name);
    List<Person> findByAgeGreaterThan(int age);
}
