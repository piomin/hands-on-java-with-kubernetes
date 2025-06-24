package pl.piomin.services.callme.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/callme")
public class CallmeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallmeController.class);

    @Value("${VERSION:v1}")
    private String version;
    @Value("${HOSTNAME:localhost}")
    private String hostname;

    @GetMapping("/ping")
    public String ping() {
        LOGGER.info("Ping: name={}, version={}", hostname, version);
        return "I'm callme-service " + hostname + ":" + version;
    }

    private final AtomicInteger index = new AtomicInteger();
    private final Random random = new Random();

    @GetMapping("/ping-with-random-error")
    public ResponseEntity<String> pingWithRandomError() {
        int r = random.nextInt(100);
        if (r % 2 == 0) {
            LOGGER.info("Ping with random error: name={}, version={}, random={}, httpCode={}",
                    hostname, version, r, HttpStatus.GATEWAY_TIMEOUT);
            return new ResponseEntity<>("Surprise " + hostname + " " + version, HttpStatus.GATEWAY_TIMEOUT);
        } else {
            LOGGER.info("Ping with random error: name={}, version={}, random={}, httpCode={}",
                    hostname, version, r, HttpStatus.OK);
            return new ResponseEntity<>("I'm callme-service " + hostname + " " + version, HttpStatus.OK);
        }
    }

    @GetMapping("/ping-with-random-delay")
    public String pingWithRandomDelay() throws InterruptedException {
        int r = new Random().nextInt(1000, 5000);
        int i = index.incrementAndGet();
        LOGGER.info("Ping with random delay: id={}, name={}, version={}, delay={}", i,
                hostname, version, r);
        Thread.sleep(r);
        return "I'm callme-service " + version;
    }

}
