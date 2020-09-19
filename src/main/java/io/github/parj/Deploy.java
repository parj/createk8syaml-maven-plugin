package io.github.parj;

import com.amihaiemil.eoyaml.YamlMapping;
import com.google.common.base.Strings;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Yaml;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Deploys deployment, service and ingress file to a Kubernetes cluster.
 *
 * By default to connect to the cluster, the app tries to pick up {@code ~/.kube/config} to be able to authenticate.
 * The location can be overriden by providing a configuration {@code kubeconfig} or by setting an environment variable
 * {@code KUBECONFIG}. If the {@code KUBECONFIG} environment variable, this will override any setting provided via pom.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class Deploy extends AbstractMojo {

    private final String KUBECONFIG = "KUBECONFIG";
    private final String defaultKubeConfigLocation = String.join(File.separator, System.getProperty("user.home"), ".kube", "config");

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * The location of the kube config file. Defaults to $HOME/.kube/config
     */
    @Parameter(property = "kubeconfig")
    String kubeconfig;

    /**
     * The kubernetes context to use to connect to the server
     */
    @Parameter(property = "context")
    String context;

    /**
     * Choose whether to deploy a deployment object. Defaults to true
     */
    @Parameter(property = "deployDeployment", defaultValue = "true")
    boolean deployDeployment;

    /**
     * Choose whether to deploy a service object. Defaults to false
     */
    @Parameter(property = "deployService", defaultValue = "false")
    boolean deployService;

    /**
     * Choose whether to deploy a ingress object. Defaults to false
     */
    @Parameter(property = "deployIngress", defaultValue = "false")
    boolean deployIngress;

    /**
     * Choose whether or not to check connection to cluster. Defaults to true by default
     */
    @Parameter(property = "checkConnection", defaultValue = "true")
    boolean checkConnection;

    /**
     * The directory containing the kubernetes files. Defaults to {@code project.build.directory}. For maven, this is
     * usually the {@code target} folder
     */
    @Parameter(property = "filesLocation", defaultValue = "${project.build.directory}")
    String filesLocation;

    /**
     * Connects to a kubernetes cluster using the {@code kubeconfig} config file. By default the file is in the location
     * {@code ~/.kube/config}. This can be overrident by setting the environment variable {@code KUBECONFIG}
     * @throws IOException If there is an issue
     * @throws ApiException If there is an issue
     */
    public void connectToCluster() throws IOException, ApiException {

        // Load kubernetes config
        if (!Strings.isNullOrEmpty(System.getenv(KUBECONFIG))) {
            kubeconfig = System.getenv(KUBECONFIG);
            getLog().info(KUBECONFIG + " system variable found. Kubernetes config file location is " + kubeconfig);
        } else
            kubeconfig = defaultKubeConfigLocation;

        getLog().info("Kubeconfig location: " + kubeconfig);

        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(kubeconfig));

        getLog().debug("Loaded config file: " + kubeconfig);

        // Select a specific context if required
        if (!Strings.isNullOrEmpty(context)) {
            getLog().info("Selecting context: " + context);
            kubeConfig.setContext(context);
        }

        ApiClient client = ClientBuilder.kubeconfig(kubeConfig).build();
        if (getLog().isDebugEnabled()) client.setDebugging(true);

        Configuration.setDefaultApiClient(client);

        if (checkConnection) {
            getLog().debug("Checking connection to cluster");
            boolean stateOfCheck = checkConnectionToCluster();
            getLog().info("Connection check to cluster is " + stateOfCheck);

            if (!stateOfCheck)
                throw new IOException("Unable to verify connection to the cluster");
        }

        getLog().debug("Kubernetes configuration has been set up");
    }

    /**
     * Tries to check connection to the cluster by getting the cluster's SSL cert
     * @return Returns true if successful or false
     * @throws ApiException  If there is an issue connecting
     */
    public boolean checkConnectionToCluster() throws ApiException {
        int numberOfPods = countSystemPods();
        getLog().debug("Number of pods in kube-system are " + numberOfPods);

        return (numberOfPods > 0);
    }

    /**
     * Deploys Kubernetes object - {@code Deployment, Service, Ingress}
     * @param name Name of the type of deployment
     * @throws IOException If there is an issue deploying
     * @throws ApiException If there is an issue deploying
     */
    public void deploy(String name) throws IOException, ApiException {
        File fileToLoad = new File(getFilePath(name));

        getLog().info("Deploying " + fileToLoad.getAbsolutePath());

        switch (name) {
            case CreateK8SYaml.DEPLOYMENT:
                deployDeployment(fileToLoad);
                break;
            case CreateK8SYaml.SERVICE:
                deployService(fileToLoad);
                break;
            case CreateK8SYaml.INGRESS:
                deployIngress(fileToLoad);
                break;
        }
    }

    /**
     * Deploys a Kuberenetes deployment object. If the deployment object is found, it is updated.
     * @param fileToLoad The file to deploy
     * @throws IOException If there is an issue deploying
     * @throws ApiException If there is an issue deploying
     */
    public void deployDeployment(File fileToLoad) throws IOException, ApiException {
        String namespace = getNamespace(fileToLoad);
        String nameOfApp = getName(fileToLoad);
        AppsV1Api appsV1Api = new AppsV1Api();

        if (countOfKubernetesObject(CreateK8SYaml.DEPLOYMENT, nameOfApp, namespace) > 0)
            appsV1Api.replaceNamespacedDeployment(nameOfApp, namespace, (V1Deployment) Yaml.load(fileToLoad), null, null, null);
        else
            appsV1Api.createNamespacedDeployment(namespace, (V1Deployment) Yaml.load(fileToLoad), null, null, null);

        getLog().info(String.join(" ", "Deployed", nameOfApp, "@ namespace", namespace) );
    }

    /**
     * Deploys a Kubernetes service object. If the service object is found, it is then deleted and then deployed
     * @param fileToLoad The file to deploy
     * @throws IOException If there is an issue deploying
     * @throws ApiException If there is an issue deploying
     */
    public void deployService(File fileToLoad) throws IOException, ApiException {
        String namespace = getNamespace(fileToLoad);
        String nameOfApp = getName(fileToLoad);
        CoreV1Api coreV1Api = new CoreV1Api();

        if (countOfKubernetesObject(CreateK8SYaml.SERVICE, nameOfApp, namespace) > 0) {
            getLog().info("Deleting service before deployment " + nameOfApp);
            //TODO - Cannot patch service directly, therefore delete it
            coreV1Api.deleteNamespacedService(nameOfApp, namespace, null, null, 90, null, null, null);
        }

        coreV1Api.createNamespacedService(namespace, (V1Service) Yaml.load(fileToLoad), null, null, null);
        getLog().info(String.join(" ", "Service", nameOfApp, "@ namespace", namespace) );
    }

    /**
     * Deploys a Kuberenetes Ingress object. If the Ingress object is found, it is updated.
     * @param fileToLoad File to load
     * @throws IOException If there is an issue deploying
     * @throws ApiException If there is an issue deploying
     */
    public void deployIngress(File fileToLoad) throws IOException, ApiException {
        String namespace = getNamespace(fileToLoad);
        String nameOfApp = getName(fileToLoad);
        NetworkingV1beta1Api networkingV1beta1Api = new NetworkingV1beta1Api();
        if (countOfKubernetesObject(CreateK8SYaml.INGRESS, nameOfApp, namespace) > 0)
            networkingV1beta1Api.replaceNamespacedIngress(nameOfApp, namespace, (NetworkingV1beta1Ingress) Yaml.load(fileToLoad), null, null, null);
        else
            networkingV1beta1Api.createNamespacedIngress(namespace, (NetworkingV1beta1Ingress)Yaml.load(fileToLoad), null, null, null);

        getLog().info(String.join(" ", "Ingress", nameOfApp, "@ namespace", namespace) );
    }

    /**
     * Counts the number of pods in the {@code kube-system} namespace
     * @return Number of pods
     * @throws ApiException If there is an issue counting the pods
     */
    public int countSystemPods() throws ApiException {
        CoreV1Api coreV1Api = new CoreV1Api();
        V1PodList v1PodList = coreV1Api.listNamespacedPod("kube-system", null, null, null, null, null, null, null, 90, null);
        return v1PodList.getItems().size();
    }

    /**
     * Returns count of Kubernetes object in the cluster
     * @param type The type of object - deployment/service/ingress
     * @param nameOfApp The name of the object
     * @param namespace The namespace of the object
     * @return The count in the cluster
     * @throws ApiException If there is an issue deploying
     */
    public int countOfKubernetesObject(String type, String nameOfApp, String namespace) throws ApiException {
        int size = 0;

        switch (type) {
            case CreateK8SYaml.DEPLOYMENT:
                AppsV1Api appsV1Api = new AppsV1Api();
                V1DeploymentList v1DeploymentList =
                        appsV1Api.listNamespacedDeployment(namespace, nameOfApp, null, null, null, null, null, null, null, null);
                size = v1DeploymentList.getItems().size();
                break;
            case CreateK8SYaml.SERVICE:
                CoreV1Api coreV1Api = new CoreV1Api();
                V1ServiceList v1ServiceList =
                        coreV1Api.listNamespacedService(namespace, nameOfApp, null, null, null, null, null, null, null, null);
                size = v1ServiceList.getItems().size();
                break;
            case CreateK8SYaml.INGRESS:
                NetworkingV1beta1Api networkingV1beta1Api = new NetworkingV1beta1Api();
                NetworkingV1beta1IngressList networkingV1beta1IngressList =
                        networkingV1beta1Api.listNamespacedIngress(namespace, nameOfApp, null, null, null, null, null, null, null, null);
                size = networkingV1beta1IngressList.getItems().size();
                break;
        }

        getLog().info(String.join(" ", "Found", Integer.toString(size), nameOfApp, "in namespace", namespace));
        return size;
    }

    /**
     * Returns name of object in the Kubernetes yaml file
     * @param file The Kubernetes file object
     * @return The name of the object
     * @throws IOException If there is an issue reading the yaml
     */
    public String getName(File file) throws IOException {
        YamlMapping yaml = com.amihaiemil.eoyaml.Yaml.createYamlInput(file, Boolean.TRUE).readYamlMapping();
        String name = yaml.yamlMapping("metadata").string("name");
        getLog().debug("Name is " + name);
        return name;
    }

    /**
     * Finds the file and then returns name of object in the Kubernetes yaml file
     * @param name The name of the file
     * @return The name of the object
     * @throws IOException If there is an issue reading the yaml
     */
    public String getName(String name) throws IOException {
        return getFilePath(name);
    }

    /**
     * Returns namespace of object in the Kubernetes yaml file
     * @param file The Kubernetes file object
     * @return The name of the object
     * @throws IOException If there is an issue reading the yaml
     */
    public String getNamespace(File file) throws IOException {
        YamlMapping yaml = com.amihaiemil.eoyaml.Yaml.createYamlInput(file, Boolean.TRUE).readYamlMapping();
        String namespace = yaml.yamlMapping("metadata").string("namespace");
        getLog().debug("Namespace is " + namespace);
        return namespace;
    }

    /**
     * Finds the file and then returns namespace of object in the Kubernetes yaml file
     * @param name The name of the file
     * @return The name of the object
     * @throws IOException If there is an issue reading the yaml
     */
    public String getNamespace(String name) throws IOException {
        return getFilePath(name);
    }

    /**
     * Returns path of the object
     * @param name Name of file
     * @return Path of the file
     */
    public String getFilePath(String name) {
        return filesLocation + File.separator + name;
    }

    /**
     * Executes deployment
     * @throws MojoExecutionException If there is an issue executing
     */
    public void execute() throws MojoExecutionException {
        try {
            getLog().debug("Trying to connect to the cluster");
            connectToCluster();

            if (deployDeployment)   deploy(CreateK8SYaml.DEPLOYMENT);
            if (deployService)      deploy(CreateK8SYaml.SERVICE);
            if (deployIngress)      deploy(CreateK8SYaml.INGRESS);

        } catch (IOException|ApiException e) {
            getLog().error(e.getMessage());
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
