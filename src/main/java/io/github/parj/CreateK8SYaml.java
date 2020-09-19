package io.github.parj;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


/**
 * Creates a minimal Kubernetes deployment, service and ingress yaml file.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE)
public class CreateK8SYaml extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Name of the Kubernetes application to use in the deployment, service and ingress.
     * By default the name provided in the pom.xml is picked up and applied.
     */
    @Parameter(property = "name", defaultValue = "${project.name}")
    String name;

    /**
     * Namespace to be used in Kubernetes. If not provided the {@code default} Kubernetes namespace is used.
     */
    @Parameter(property = "namespace", required = true, defaultValue = "default")
    String namespace;

    /**
     * The application port to be exposed. If not provided {@code 8080} is exposed by default
     */
    @Parameter(property = "port", defaultValue = "8080")
    String port;

    /**
     * The docker image registry url to use. Example {@code parjanya/samplespringbootapp}
     */
    @Parameter(property = "image", required = true)
    String image;

    /**
     * The end point of the application to be exposed. Example {@code /foo/bar}
     */
    @Parameter(property = "path", required = true)
    String path;

    /**
     * The hostname to be used in the ingress. If not provided, {@code localhost} is provided as default
     */
    @Parameter(property = "host", required = true, defaultValue = "localhost")
    String host;

    /**
     * Path for the readiness probe for Kubernetes. Ex. {@code /hello/actuator/health}.
     * Readiness is used to check if the deployment pod is ready to receive traffic.
     * If this is not set, the block of yaml driving readiness will not be included.
     *
     * cf - https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
     * https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot
     */
    @Parameter(property = "readinessProbePath")
    String readinessProbePath;

    /**
     * Path for the liveness probe for Kubernetes. Ex. {@code /hello/actuator/customHealthCheck}.
     * Liveness is used to check if the pod is functioning
     * If this is not set, the block of yaml driving liveness will not be included.
     *
     * cf - https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
     * https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot
     */
    @Parameter(property = "livenessProbePath")
    String livenessProbePath;

    /**
     * The location where the template kubernetes files are sitting. If not provided, will default to the resources folder
     */
    @Parameter(property = "inputDirectory")
    String inputDirectory;

    /**
     * The location where the kuberentes files should be written to. If not provided, will default to the project build folder
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    String outputDirectory;

    /**
     * Points to the Kubernetes deployment yaml sitting in the {@code resources folder}. https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
     */
    public static final String DEPLOYMENT = "deployment.yaml";

    /**
     * Points to the Kubernetes service yaml sitting in the {@code resources folder}. https://kubernetes.io/docs/concepts/services-networking/service/
     */
    public static final String SERVICE    = "service.yaml";

    /**
     * Points to the Kubernetes ingress yaml sitting in the {@code resources folder}. https://kubernetes.io/docs/concepts/services-networking/ingress/
     */
    public static final String INGRESS    = "ingress.yaml";

    /**
     * Renders the Kubernetes files. The template files are under the {@code resources} folder. There are three files
     * in there - {@code deployment.yaml}, {@code service.yaml}, {@code ingress.yaml}. Jinja templating is used to
     * plug in the values and the files are written to the {@code target} folder
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        Map<String, Object> context = getMap();
        try {
            renderTemplate(DEPLOYMENT, context);
            renderTemplate(SERVICE, context);
            renderTemplate(INGRESS, context);
        } catch (IOException e) {
            getLog().error(e.getMessage());
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Creates a hashmap for the jinja templating
     * @return Hashmap with values to render the jinja template
     */
    private Map<String, Object> getMap() {
        Map<String, Object> context = Maps.newHashMap();

        context.put("name", name.toLowerCase());
        context.put("namespace", namespace);
        context.put("port", port);
        context.put("image", image);
        context.put("path", path);
        context.put("host", host);
        context.put("readinessProbePath", readinessProbePath);
        context.put("livenessProbePath", livenessProbePath);

        return context;
    }

    /**
     * Renders using jinja templating
     * @param nameOfResource The name of the file to render - ex. {@code deployment.yaml}, {@code service.yaml}, {@code ingress.yaml}
     * @param context Hashmap for jinja templating
     * @return The rendered template is also returned as a string
     * @throws IOException Throws exception if the files cannot be created/written to.
     */
    public String renderTemplate(String nameOfResource, Map<String, Object> context) throws IOException {
        Jinjava jinjava = new Jinjava();
        String template;
        if (Strings.isNullOrEmpty(inputDirectory)) {
            getLog().info("Picking up file " + Resources.getResource(nameOfResource));
            template = Resources.toString(Resources.getResource(nameOfResource), Charsets.UTF_8);
        }
        else {
            String filePath = inputDirectory + File.separator + nameOfResource;
            getLog().info("Picking up file " + filePath);
            template = new String(Files.readAllBytes(Paths.get(inputDirectory + File.separator + nameOfResource)));
        }

        String renderedTemplate = jinjava.render(template, context);
        getLog().info("Creating " + nameOfResource);

        writeToFile(nameOfResource, renderedTemplate);
        getLog().debug("Yaml of " + nameOfResource + " is :" + renderedTemplate.toString());

        return  renderedTemplate;
    }

    /**
     * Writes the rendered template to the file to the {@code outputDirectory} folder
     * @param file The name of the file to write to - ex. {@code deployment.yaml}, {@code service.yaml}, {@code ingress.yaml}
     * @param contents Contents of the yaml file
     * @throws IOException Throws exception if the file cannot be created/written to.
     */
    private void writeToFile(String file, String contents) throws IOException {
        String fileName = outputDirectory + File.separator + file;

        getLog().info("Writing to " + fileName);
        Path path = Paths.get(fileName);
        byte[] strToBytes = contents.getBytes();
        Files.write(path, strToBytes);
    }
}
