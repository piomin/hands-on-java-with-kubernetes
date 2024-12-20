package pl.piomin.services.caller.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/caller")
public class CallerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallerController.class);

    private final RestClient restClient;
    @Value("${VERSION:v1}")
    private String version;

    public CallerController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/ping")
    public String ping() {
        LOGGER.info("Ping: version={}", version);
        String response = restClient.get()
                .uri("http://callme-service:8080/callme/ping")
                .retrieve()
                .body(String.class);
        LOGGER.info("Calling: response={}", response);
        return "I'm caller-service " + version + ". Calling... " + response;
    }

    @GetMapping("/ping-with-random-error")
    public ResponseEntity<String> pingWithRandomError() {
        LOGGER.info("Ping with random error: version={}", version);
        ResponseEntity<String> responseEntity = restClient.get()
                .uri("http://callme-service:8080/callme/ping-with-random-error")
                .retrieve()
                .toEntity(String.class);
        LOGGER.info("Calling: responseCode={}, response={}", responseEntity.getStatusCode(), responseEntity.getBody());
        return new ResponseEntity<>("I'm caller-service " + version + ". Calling... " +
                responseEntity.getBody(), responseEntity.getStatusCode());
    }

    @GetMapping("/ping-with-random-delay")
    public ResponseEntity<String> pingWithRandomDelay() {
        LOGGER.info("Ping with random delay: version={}", version);
        ResponseEntity<String> responseEntity = restClient.get()
                .uri("http://callme-service:8080/callme/ping-with-random-delay")
                .retrieve()
                .toEntity(String.class);
        LOGGER.info("Calling: responseCode={}, response={}", responseEntity.getStatusCode(), responseEntity.getBody());
        return new ResponseEntity<>("I'm caller-service " + version + ". Calling... " +
                responseEntity.getBody(), responseEntity.getStatusCode());
    }

}
