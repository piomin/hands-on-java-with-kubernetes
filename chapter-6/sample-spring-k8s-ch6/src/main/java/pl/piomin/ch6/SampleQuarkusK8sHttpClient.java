package pl.piomin.ch6;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface SampleQuarkusK8sHttpClient {

    @GetExchange("/hello")
    String hello();
}
