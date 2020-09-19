package io.github.parj;

import io.kubernetes.client.openapi.ApiException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestKubernetesDeployment {
    public Deploy deploy;

    @Before
    public void init() throws IOException, ApiException {
        deploy = new Deploy();
        deploy.connectToCluster();
        MavenProject project = new MavenProject();
        Build build = new Build();
        build.setOutputDirectory("./target");
        project.setBuild(build);
        deploy.project = project;
        deploy.filesLocation = build.getOutputDirectory();

    }

    @Test
    public void test_1_Deployment() throws IOException, ApiException {
        deploy.deploy(CreateK8SYaml.DEPLOYMENT);
    }

    @Test
    public void test_2_Service() throws IOException, ApiException {
        deploy.deploy(CreateK8SYaml.SERVICE);
    }

    @Test
    public void test_3_Ingress() throws IOException, ApiException {
        deploy.deploy(CreateK8SYaml.INGRESS);
    }
}
