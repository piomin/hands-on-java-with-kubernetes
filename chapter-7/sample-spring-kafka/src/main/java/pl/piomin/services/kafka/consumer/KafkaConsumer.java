package pl.piomin.services.kafka.consumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;

@SpringBootApplication
@EnableKafka
public class KafkaConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumer.class);

    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumer.class, args);
    }

    @Value("${app.in.topic}")
    private String topic;

    @Bean
    public NewTopic infoTopic() {
        return TopicBuilder.name(topic)
                .partitions(10)
                .replicas(3)
                .build();
    }

}
