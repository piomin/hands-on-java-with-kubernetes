package pl.piomin.ch9.repository;

import org.springframework.data.repository.CrudRepository;
import pl.piomin.ch9.domain.Person;

import java.util.List;

public interface PersonRepository extends CrudRepository<Person, Integer> {
    List<Person> findByAgeGreaterThan(int age);
}
