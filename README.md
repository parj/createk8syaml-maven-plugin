
[![GitHub license](https://img.shields.io/github/license/parj/createk8syaml-maven-plugin.svg)](https://github.com/parj/createk8syaml-maven-plugin/blob/main/LICENSE)
# createk8syaml-maven-plugin

This is a simple maven plugin to create a Kuberenetes deployment, service and ingress yaml file. The files can then be used to the application to kubernetes.

When you run the plugin, the files are written out to the `target` folder.

To use this

    <build>
        <plugins>
        ....
            <plugin>
                <groupId>io.github.parj</groupId>
                <artifactId>createk8syaml-maven-plugin</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <configuration>
                    <namespace>thisisaspace</namespace>
                    <image>gcr.io/etc</image>
                    <path>/foo</path>
                    <host>localhost</host>
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


Parameters
-----------

| Parameter Name | Defaults | Description |
| -------------- | -------- | ----------- |
| `name` | The name of the project is taken, this is converted to lowercase | The name of the application. By default, the name of the project is taken, this is converted to lowercase and then plugged in |
| `namespace` | `default` | Name of the Kubernetes application to use in the deployment, service and ingress. If not provided, this defaults to `default`. |
| `port` | `8080` | The application port to be exposed. If not provided `8080` is exposed by default |
| `image` |  | The docker image registry url to use. Example: `parjanya/samplespringbootapp`|
| `path` |  | The end point of the application to be exposed. Example `/foo/bar` |
| `host` | `localhost` | The host for the ingress. If not provided `localhost` is provided |
 
