package pl.redhat.samples.quarkus.person.resource;

import org.jboss.logging.Logger;
import pl.redhat.samples.quarkus.person.model.Person;
import pl.redhat.samples.quarkus.person.repository.PersonRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;

@Path("/persons")
public class PersonResource {

    private Logger log;
    private PersonRepository personRepository;

    public PersonResource(PersonRepository personRepository, Logger log) {
        this.personRepository = personRepository;
        this.log = log;
    }

    @POST
    @Transactional
    public Person addPerson(Person person) {
        personRepository.persist(person);
        return person;
    }

    @GET
    public List<Person> getPersons() {
        log.info("getPersons");
        return personRepository.listAll();
    }

    @GET
    @Path("/name/{name}")
    public List<Person> getPersonsByName(@PathParam("name") String name) {
        return personRepository.findByName(name);
    }

    @GET
    @Path("/age-greater-than/{age}")
    public List<Person> getPersonsByAge(@PathParam("age") int age) {
        return personRepository.findByAgeGreaterThan(age);
    }

    @GET
    @Path("/{id}")
    public Person getPersonById(@PathParam("id") Long id) {
        return personRepository.findById(id);
    }

}
