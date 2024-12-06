package pl.piomin.ch9;

import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import pl.piomin.ch9.controller.PersonController;
import pl.piomin.ch9.domain.Person;
import pl.piomin.ch9.repository.PersonRepository;
import pl.piomin.ch9.repository.PersonViewRepository;
import pl.piomin.ch9.view.PersonView;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonController.class)
public class PersonControllerUnitTests {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    MockMvc mvc;
    @MockBean
    PersonRepository repository;
    @MockBean
    PersonViewRepository viewRepository;

    @Test
    public void testAdd() throws Exception {
        Person person = Instancio.of(Person.class).create();
        when(repository.save(Mockito.any(Person.class)))
                .thenReturn(person);
        mvc.perform(post("/").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(person)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(person.getId())));
    }

    @Test
    public void testFind() throws Exception {
        PersonView person = Instancio.of(PersonView.class).create();
        when(viewRepository.findOne(person.getId())).thenReturn(person);
        mvc.perform(get("/{}", person.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(person.getName())));
    }


}
