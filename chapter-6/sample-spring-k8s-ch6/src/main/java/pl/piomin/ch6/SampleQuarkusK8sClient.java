package pl.piomin.ch6;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface SampleQuarkusK8sClient {

    @GetExchange("/hello")
    String hello();
}
