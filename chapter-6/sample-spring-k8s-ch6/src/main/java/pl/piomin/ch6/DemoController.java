package pl.piomin.ch6;

import io.fabric8.kubernetes.api.model.Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.cloud.kubernetes.commons.AbstractKubernetesInfoContributor;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/hello")
public class DemoController {

    private PodUtils<Pod> utils;
    private DemoProperties properties;
    private SampleQuarkusK8sClient client;

    private long delay;

    public DemoController(DemoProperties properties,
                          PodUtils<Pod> utils,
                          SampleQuarkusK8sClient client) {
        this.delay = new Random().nextLong(50, 200);
        this.properties = properties;
        this.utils = utils;
        this.client = client;
    }

    @GetMapping
    public String hello() {
        return "Hello World from Spring Boot!";
    }

    @GetMapping("/delayed")
    public String delayed() throws InterruptedException {
        Thread.sleep(delay);
        return "Hello with delay " + delay + "ms inside " + utils.currentPod().get().getMetadata().getName();
    }

    @GetMapping("/custom")
    public Map<String, String> custom() {
        return Map.of("customProperty", properties.getProperty(),
                      "customSecureProperty", properties.getSecureProperty());
    }

    @GetMapping("/to-quarkus")
    public String callQuarkus()  {
        return client.hello();
    }
}
