package pl.piomin.services.caller;

//import brave.Tracing;
//import brave.baggage.BaggageField;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class CallerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CallerApplication.class, args);
	}

	@Bean
	RestClient restClient(RestClient.Builder restClientBuilder) {
		return restClientBuilder.build();
	}
	
}
