package pl.piomin;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/hello")
@RegisterRestClient(configKey = "spring")
public interface SpringBootAppClient {

    @GET
    String hello();
}
