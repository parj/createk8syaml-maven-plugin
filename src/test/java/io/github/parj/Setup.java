package io.github.parj;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.google.common.collect.Maps;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.Map;

public class Setup {
    public static Map<String, Object> createHashMap() {
        Map<String, Object> context = Maps.newHashMap();
        context.put("name"      , "foo");
        context.put("namespace" , "thisisanamespace");
        context.put("port"      , "5555");
        context.put("image"     , "gcr.io/distroless/java");
        context.put("path"      , "/cowjumpingoverthemoon");
        context.put("host"      , "localhost");
        return context;
    }

    public static YamlMapping readYaml(String type) throws IOException {

        CreateK8SYaml createK8SYaml = new CreateK8SYaml();
        createK8SYaml.project = new MavenProject();
        Build build = new Build();
        build.setDirectory("./target");

        createK8SYaml.project.setName("moo");
        createK8SYaml.project.setBuild(build);

        YamlMapping yaml = Yaml
                .createYamlInput(createK8SYaml.renderTemplate(type, createHashMap()), Boolean.TRUE)
                .readYamlMapping();
        return yaml;
    }
}
