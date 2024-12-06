package pl.piomin.ch9.repository;

import com.blazebit.persistence.spring.data.repository.EntityViewRepository;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.ch9.view.PersonView;

@Transactional(readOnly = true)
public interface PersonViewRepository extends EntityViewRepository<PersonView, Integer> {
    PersonView findByAgeGreaterThan(int age);
}
