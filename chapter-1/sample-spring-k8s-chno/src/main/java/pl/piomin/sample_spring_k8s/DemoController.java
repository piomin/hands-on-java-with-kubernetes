package pl.piomin.sample_spring_k8s;

//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//import java.util.Random;
//import java.util.random.RandomGenerator;

//@RestController
//@RequestMapping("/hello")
public class DemoController {

//    @Value("${POD_NAME}")
//    private String podName;
//
//    private long delay;
//
//    public DemoController() {
//        Random r = new Random();
//        r.nextLong(50, 200);
//    }
//
//    @GetMapping
//    public String hello() {
//        return "Hello World from Spring Boot!";
//    }
//
//    @GetMapping("/delayed")
//    public String delayed() throws InterruptedException {
//        Thread.sleep(delay);
//        return "Hello with delay + " + " inside " + podName;
//    }
//
//    @Value("${custom.property}")
//    private String customProperty;
//    @Value("${custom.secure-property:NOT_FOUND}")
//    private String customSecureProperty;
//
//    @GetMapping("/custom")
//    public Map<String, String> custom() {
//        return Map.of("customProperty", customProperty,
//                      "customSecureProperty", customSecureProperty);
//    }
}
