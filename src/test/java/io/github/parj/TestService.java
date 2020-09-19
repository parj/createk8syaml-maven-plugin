package io.github.parj;

import com.amihaiemil.eoyaml.YamlMapping;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestService {
    private static YamlMapping yaml;

    @BeforeClass
    public static void setUp() throws IOException {
        yaml = Setup.readYaml(CreateK8SYaml.SERVICE, true);
    }

    @Test
    public void testCompareString() {
        assertEquals("apiVersion: v1\n" +
                "kind: Service\n" +
                "metadata:\n" +
                "  name: \"foo-service\"\n" +
                "  namespace: thisisanamespace\n" +
                "spec:\n" +
                "  selector:\n" +
                "    app: foo\n" +
                "  ports:\n" +
                "    -\n" +
                "      port: 5555\n" +
                "      targetPort: 5555\n" +
                "      protocol: TCP\n" +
                "  type: ClusterIP", yaml.toString());
    }

    @Test
    public void testName() {
        assertEquals("foo-service", yaml.yamlMapping("metadata").string("name"));
    }

    @Test
    public void testNamespace() {
        assertEquals("thisisanamespace", yaml.yamlMapping("metadata").string("namespace"));
    }

    @Test
    public void testSelectorLabel() {
        assertEquals("foo", yaml
                .yamlMapping("spec")
                .yamlMapping("selector")
                .string("app"));
    }

    @Test
    public void testPort() {
        assertEquals("5555", yaml
                .yamlMapping("spec")
                .yamlSequence("ports")
                .yamlMapping(0)
                .string("port"));

        assertEquals(5555, yaml
                .yamlMapping("spec")
                .yamlSequence("ports")
                .yamlMapping(0)
                .integer("targetPort"));
    }
}
