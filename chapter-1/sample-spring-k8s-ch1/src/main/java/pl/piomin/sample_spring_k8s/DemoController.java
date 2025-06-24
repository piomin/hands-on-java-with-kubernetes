package pl.piomin.sample_spring_k8s;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World from Spring Boot!";
    }

}

