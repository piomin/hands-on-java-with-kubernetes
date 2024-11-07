package pl.piomin.sample_spring_k8s;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/hello")
public class DemoController {

    @GetMapping
    public String hello() {
        return "Hello World from Spring Boot!";
    }

    @Value("${custom.property}")
    private String customProperty;
    @Value("${custom.secure-property:NOT_FOUND}")
    private String customSecureProperty;

    @GetMapping("/custom")
    public Map<String, String> custom() {
        return Map.of("customProperty", customProperty,
                      "customSecureProperty", customSecureProperty);
    }
}
