package pl.piomin.ch9.repository;

import org.springframework.data.repository.CrudRepository;
import pl.piomin.ch9.domain.Person;

public interface PersonRepository extends CrudRepository<Person, Integer> {
}
