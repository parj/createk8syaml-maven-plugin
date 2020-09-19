 [![CircleCI](https://circleci.com/gh/parj/createk8syaml-maven-plugin.svg?style=svg)](https://circleci.com/gh/parj/createk8syaml-maven-plugin) [![GitHub license](https://img.shields.io/github/license/parj/createk8syaml-maven-plugin.svg)](https://github.com/parj/createk8syaml-maven-plugin/blob/main/LICENSE) [![Known Vulnerabilities](https://snyk.io/test/github/parj/createk8syaml-maven-plugin/badge.svg)](https://snyk.io/test/github/parj/createk8syaml-maven-plugin) [![DepShield Badge](https://depshield.sonatype.org/badges/parj/createk8syaml-maven-plugin/depshield.svg)](https://depshield.github.io) [![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fparj%2Fcreatek8syaml-maven-plugin.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fparj%2Fcreatek8syaml-maven-plugin?ref=badge_shield)

# createk8syaml-maven-plugin

This is a simple maven plugin to create a Kubernetes deployment, service and ingress yaml file. The files can then be used to deploy the application to kubernetes.

When you run the plugin, the files are written out to the `target` folder. Documentation has been published to → https://parj.github.io/createk8syaml-maven-plugin/.

## Generating the kubernetes file

The plugin can be triggered via `mvn createk8syaml:deploy`

```xml
            <plugin>
                <groupId>io.github.parj</groupId>
                <artifactId>createk8syaml-maven-plugin</artifactId>
                <version>0.0.5</version>
                <configuration>
                    <!-- Mandatory -->
                    <namespace>thisisaspace</namespace>
                    <image>gcr.io/etc</image>
                    <path>/foo</path>

                    <!-- Optional -->
                    <host>localhost</host>
                    <name>arandomname</name>
                    <port>9999</port>
                    <readinessProbePath>/foo/actuator/health</readinessProbePath>
                    <livenessProbePath>/foo/actuator/live</livenessProbePath>
                </configuration>
                <executions>
                    <execution>
                      <phase>package</phase>
                      <goals>
                          <goal>createk8syaml</goal>
                      </goals>
                    </execution>
                </executions>
            </plugin>
```

### Parameters

| Parameter Name | Defaults | Description |
| -------------- | -------- | ----------- |
| `name` | The name of the project is taken, this is converted to lowercase | The name of the application. By default, the name of the project is taken, this is converted to lowercase and then plugged in |
| `namespace` | `default` | Name of the Kubernetes application to use in the deployment, service and ingress. If not provided, this defaults to `default`. |
| `port` | `8080` | The application port to be exposed. If not provided `8080` is exposed by default |
| `image` |  | The docker image registry url to use. Example: `parjanya/samplespringbootapp`|
| `path` |  | The end point of the application to be exposed. Example `/foo/bar` |
| `host` | `localhost` | The host for the ingress. If not provided `localhost` is provided |
| `readinessProbePath` |  | Path for the readiness probe for Kubernetes. Ex. `/hello/actuator/health`.|
| `livenessProbePath` |  | Path for the liveness probe for Kubernetes. Ex. `/hello/actuator/customHealthCheck`|
 
### Using with Jib/Fabric/Spotify Docker plugin

If you are using [jib](https://github.com/GoogleContainerTools/jib) or [fabric8](https://github.com/fabric8io/docker-maven-plugin) or [spotify docker plugin](https://github.com/spotify/dockerfile-maven), define the docker image name as a variable and then use it both places. 

Within the `properties` section of the pom.xml, define a variable to hold the docker image name

```xml
    <properties>
        <docker.image>docker.io/parjanya/${project.name}:${project.version}</docker.image>
    </properties>
```

In the plugin, use it as follows

```xml
            <plugin>
                <groupId>io.github.parj</groupId>
                <artifactId>createk8syaml-maven-plugin</artifactId>
                <version>0.0.5</version>
                <configuration>
                    <namespace>thisisaspace</namespace>
                    <image>${docker.image}</image>
                    <path>/foo</path>
                </configuration>
                <executions>
                    <execution>
                      <phase>package</phase>
                      <goals>
                          <goal>generate****</goal>
                      </goals>
                    </execution>
                </executions>
            </plugin>
```
and if you are using one of the plugins to create the docker image, the below example is for [jib](https://github.com/GoogleContainerTools/jib).

```xml
            <plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
                <version>2.5.2</version>
                <configuration>
                    <from>
                        <image>gcr.io/distroless/java:11</image>
                    </from>
                    <to>
                        <image>${docker.image}</image>
                    </to>
                </configuration>
            </plugin>
```

## Deploying to Kubernetes

The plugin can also be used to deploy to Kubernetes. The application uses the `~/.kube/config` to get the connection details of the cluster

This can be triggered by running `mvn createk8syaml:deploy`

```xml
    <build>
        <plugins>
        ....
            <plugin>
                <groupId>io.github.parj</groupId>
                <artifactId>createk8syaml-maven-plugin</artifactId>
                <version>0.0.5</version>
                <!-- Optional -->
                <configuration>
                    <kubeconfig>~/randomplace/.config</kubeconfig>
                    <context>minikube</context>
                    <deployService>true</deployService>
                    <deployIngress>true</deployIngress>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>deploy</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
```

### Parameters

| Parameter Name | Defaults | Description |
| -------------- | -------- | ----------- |
| `kubeconfig` | `~/.kube/config` | The path to the kuberenetes config file|
| `context` | Current context within Kubernetes config | The context to use withing the Kubernetes config. Used if you are connecting to a different server that is not the default one |
| `deployDeployment` | `true` | Indicate whether or not to deploy the Kubernetes deployment object |
| `deployService` | `false` | Indicate whether or not to deploy the Kubernetes service object |
| `deployIngress` | `false` | Indicate whether or not to deploy the Kubernetes ingress object |
| `checkConnection` | `true` | If toggled off, there will no connection check before deployment |
| `filesLocation` | `${project.build.outputDirectory}` | By default the maven build output directory is picked up |