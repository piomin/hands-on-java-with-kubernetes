package pl.piomin.services.kubernetes.vault.repository;

import org.springframework.data.repository.CrudRepository;
import pl.piomin.services.kubernetes.vault.model.Person;

public interface PersonRepository extends CrudRepository<Person, Integer> {
}
