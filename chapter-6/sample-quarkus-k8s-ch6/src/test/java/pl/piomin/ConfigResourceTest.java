package pl.piomin;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@WithKubernetesTestServer(setup = ConfigResourceTest.Setup.class)
public class ConfigResourceTest {

    public static class Setup implements Consumer<KubernetesServer> {

        @Override
        public void accept(KubernetesServer server) {
            ConfigMapBuilder cb = new ConfigMapBuilder();
            cb.withNewMetadata().withName("sample-quarkus-k8s").endMetadata();
            cb.addToData(Map.of("application.properties", "simple.property = 123"));
            server.getClient().configMaps().create(cb.build());

            SecretBuilder sb = new SecretBuilder();
            sb.withNewMetadata().withName("sample-quarkus-k8s").endMetadata();
            sb.addToData(Map.of("application.properties", "c2VjdXJlLnByb3BlcnR5ID0gNDU2"));
            server.getClient().secrets().create(sb.build());
        }
    }

    @Test
    public void verifyConfig() {
        RestAssured.when().get("/config")
                .then()
                .statusCode(200)
                .body("customProperty", equalTo("123"))
                .body("customSecureProperty", equalTo("456"));
    }

}
