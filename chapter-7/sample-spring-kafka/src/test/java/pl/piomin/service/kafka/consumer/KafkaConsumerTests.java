package pl.piomin.service.kafka.consumer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import pl.piomin.services.kafka.consumer.KafkaConsumer;
import pl.piomin.services.kafka.consumer.ListenerService;
import pl.piomin.services.kafka.consumer.message.Info;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SpringBootTest(properties = {"spring.kafka.consumer.auto-offset-reset=earliest"},
                classes = {KafkaConsumer.class, ListenerService.class})
@EmbeddedKafka(topics = {"info"},
        partitions = 10,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class KafkaConsumerTests {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerTests.class);

    @Autowired
    private EmbeddedKafkaBroker kafka;
    @Autowired
    private KafkaTemplate<Long, Info> template;

    @Test
    void eventReceive() throws ExecutionException, InterruptedException, TimeoutException {
//        Info info = new Info(1L, "", "", "", "");
//        SendResult<Long, Info> r = template.send("info", info.getId(), info)
//                .get(1000, TimeUnit.MILLISECONDS);
//        LOG.info("Sent: {}", r.getProducerRecord().value());
    }
}
