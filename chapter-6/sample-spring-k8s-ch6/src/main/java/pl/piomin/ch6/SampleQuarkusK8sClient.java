package pl.piomin.ch6;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("sample-quarkus-k8s")
public interface SampleQuarkusK8sClient {

    @GetMapping("/hello")
    public String hello();
}
