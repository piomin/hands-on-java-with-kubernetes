package pl.piomin.services.caller.config;

import brave.Tracing;
import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

    @Bean
    public Tracing tracing() {

        return Tracing.newBuilder()
                .propagationFactory(BaggagePropagation.newFactoryBuilder(new W3CPropagation())
                   .add(BaggagePropagationConfig.SingleBaggageField.newBuilder(BaggageField.create("X-Version"))
                       .addKeyName("X-Version")
                       .build())
                   .build())
                .build();
    }

}
