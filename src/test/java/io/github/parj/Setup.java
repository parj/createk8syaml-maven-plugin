package io.github.parj;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.common.collect.Maps;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.Map;

public class Setup {
    public static Map<String, Object> createHashMap(boolean includeReadinessLiveness) {
        Map<String, Object> context = Maps.newHashMap();
        context.put("name"              , "foo");
        context.put("namespace"         , "thisisanamespace");
        context.put("port"              , "5555");
        context.put("image"             , "gcr.io/distroless/java");
        context.put("path"              , "/cowjumpingoverthemoon");
        context.put("host"              , "localhost");
        if (includeReadinessLiveness) {
            context.put("readinessProbePath", "/cowjumpingoverthemoon/actuator/health");
            context.put("livenessProbePath" , "/cowjumpingoverthemoon/actuator/live");
        }
        return context;
    }

    public static YamlMapping readYaml(String type, boolean includeReadinessLiveness) throws IOException {

        CreateK8SYaml createK8SYaml = new CreateK8SYaml();
        createK8SYaml.project = new MavenProject();
        Build build = new Build();
        build.setOutputDirectory("./target");

        createK8SYaml.project.setName("moo");
        createK8SYaml.project.setBuild(build);
        createK8SYaml.outputDirectory = build.getOutputDirectory();

        YamlMapping yaml = Yaml
                .createYamlInput(createK8SYaml.renderTemplate(type, createHashMap(includeReadinessLiveness)), Boolean.TRUE)
                .readYamlMapping();
        return yaml;
    }
}
