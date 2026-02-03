package pl.piomin.sample_spring_k8s;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DemoTestsIT {

    KubernetesClient client = new KubernetesClientBuilder().build();

//    @Test
    void shouldBeDeployed() {
        Deployment deployment = client.apps().deployments()
                .inNamespace("default")
                .withName("sample-spring-k8s-ch1")
                .get();
        Assertions.assertNotNull(deployment);
    }

//    @Test
    void shouldRunning() {
        List<Pod> pods = client.pods()
                .inNamespace("default")
                .withLabel("app.kubernetes.io/name", "sample-spring-k8s-ch1")
                .list()
                .getItems();
        Assertions.assertEquals(1, pods.size());
    }
}
