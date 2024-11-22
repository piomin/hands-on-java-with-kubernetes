package pl.piomin;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@Path("/config")
public class ConfigResource {

    @ConfigProperty(name = "simple.property", defaultValue = "NOT_FOUND")
    private String customProperty;

    @ConfigProperty(name = "secure.property", defaultValue = "NOT_FOUND")
    private String customSecureProperty;

    @GET
    public Map<String, String> displayConfig() {
        return Map.of("customProperty", customProperty,
                "customSecureProperty", customSecureProperty);
    }
}
