package pl.piomin;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;

@Path("/hello")
public class GreetingResource {


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @ConfigProperty(name = "custom.property")
    private String customProperty;

    @ConfigProperty(name = "custom.secure-property", defaultValue = "NOT_FOUND")
    private String customSecureProperty;

    @GET
    @Path("/custom")
    public Map<String, String> custom() {
        return Map.of("customProperty", customProperty,
                "customSecureProperty", customSecureProperty);
    }

    @Inject
    @RestClient
    SpringBootAppClient client;

    @GET
    @Path("/to-spring")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloToSpring() {
        return client.hello();
    }

}
