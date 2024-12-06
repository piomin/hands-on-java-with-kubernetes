package pl.piomin.ch9.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import pl.piomin.ch9.domain.Person;

@EntityView(Person.class)
public interface PersonView {

    @IdMapping
    Integer getId();
    void setId(Integer id);

    @Mapping("CONCAT(firstName,' ',lastName)")
    String getName();
    void setName(String name);
}
