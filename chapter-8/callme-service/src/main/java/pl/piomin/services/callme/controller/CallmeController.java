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

    private AtomicInteger index = new AtomicInteger();

//    @GetMapping("/ping-with-random-error")
//    public ResponseEntity<String> pingWithRandomError() {
//        int r = random.nextInt(100);
//        if (r % 2 == 0) {
//            LOGGER.info("Ping with random error: name={}, version={}, random={}, httpCode={}",
//                    buildProperties.isPresent() ? buildProperties.get().getName() : "callme-service", version, r, HttpStatus.GATEWAY_TIMEOUT);
//            return new ResponseEntity<>("Surprise " + INSTANCE_ID + " " + version, HttpStatus.GATEWAY_TIMEOUT);
//        } else {
//            LOGGER.info("Ping with random error: name={}, version={}, random={}, httpCode={}",
//                    buildProperties.isPresent() ? buildProperties.get().getName() : "callme-service", version, r, HttpStatus.OK);
//            return new ResponseEntity<>("I'm callme-service" + INSTANCE_ID + " " + version, HttpStatus.OK);
//        }
//    }

//    @GetMapping("/ping-with-random-delay")
//    public String pingWithRandomDelay() throws InterruptedException {
//        int r = new Random().nextInt(3000);
//        int i = index.incrementAndGet();
//        ProcessingEvent event = new ProcessingEvent(i);
//        event.begin();
//        LOGGER.info("Ping with random delay: id={}, name={}, version={}, delay={}", i,
//                buildProperties.isPresent() ? buildProperties.get().getName() : "callme-service", version, r);
//        Thread.sleep(r);
//        event.commit();
//        return "I'm callme-service " + version;
//    }

}
