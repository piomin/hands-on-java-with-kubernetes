package pl.piomin;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
public class PersonTestOnKubernetesIT {

    @Inject
    KubernetesClient client;

    @Test
    void shouldBeDeployed() {
        Deployment deployment = client.apps().deployments()
                .withName("sample-quarkus-on-kubernetes")
                .get();
        Assertions.assertNotNull(deployment);
    }

    @Test
    void shouldRunning() {
        List<Pod> pods = client.pods()
                .withLabel("app.kubernetes.io/name", "sample-quarkus-on-kubernetes")
                .list()
                .getItems();
        Assertions.assertEquals(1, pods.size());
    }
}
