package io.github.parj;

import com.google.common.base.Charsets;
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
@Mojo(name = "createk8syaml", defaultPhase = LifecyclePhase.PACKAGE)
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
            System.err.println(e.getMessage());
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
        String template = Resources.toString(Resources.getResource(nameOfResource), Charsets.UTF_8);
        String renderedTemplate = jinjava.render(template, context);
        getLog().info("Creating " + nameOfResource);

        writeToFile(nameOfResource, renderedTemplate);
        getLog().debug("Yaml of " + nameOfResource + " is :" + renderedTemplate.toString());

        return  renderedTemplate;
    }

    /**
     * Writes the rendered template to the file to the {@code target folder
     * @param file The name of the file to write to - ex. {@code deployment.yaml}, {@code service.yaml}, {@code ingress.yaml}
     * @param contents
     * @throws IOException Throws exception if the file cannot be created/written to.
     */
    private void writeToFile(String file, String contents) throws IOException {
        String fileName = project.getBuild().getDirectory() + File.separator + file;

        getLog().info("Writing to " + fileName);
        Path path = Paths.get(fileName);
        byte[] strToBytes = contents.getBytes();
        Files.write(path, strToBytes);
    }
}
