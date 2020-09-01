package io.github.parj;

import com.amihaiemil.eoyaml.YamlMapping;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestIngress  {
    private static YamlMapping yaml;

    @Before
    public void setUp() throws IOException {
        yaml = Setup.readYaml(CreateK8SYaml.INGRESS);
    }

    @Test
    public void testCompareString() {

        assertEquals("apiVersion: networking.k8s.io/v1beta1\n" +
                "kind: Ingress\n" +
                "metadata:\n" +
                "  name: \"foo-ingress\"\n" +
                "  namespace: thisisanamespace\n" +
                "spec:\n" +
                "  rules:\n" +
                "    -\n" +
                "      host: localhost\n" +
                "      http:\n" +
                "        paths:\n" +
                "          -\n" +
                "            path: /cowjumpingoverthemoon\n" +
                "            backend:\n" +
                "              serviceName: \"foo-service\"\n" +
                "              servicePort: 5555", yaml.toString());
    }

    @Test
    public void testName() {
        assertEquals("foo-ingress", yaml.yamlMapping("metadata").string("name"));
    }

    @Test
    public void testNamespace() {
        assertEquals("thisisanamespace", yaml.yamlMapping("metadata").string("namespace"));
    }

    @Test
    public void testHost() {
        assertEquals("localhost", yaml
                .yamlMapping("spec")
                .yamlSequence("rules")
                .yamlMapping(0)
                .string("host"));
    }

    @Test
    public void testBackend() {
        YamlMapping backend = yaml
                .yamlMapping("spec")
                .yamlSequence("rules")
                .yamlMapping(0)
                .yamlMapping("http")
                .yamlSequence("paths")
                .yamlMapping(0)
                .yamlMapping("backend");
        assertEquals("foo-service", backend.string("serviceName"));
        assertEquals(5555, backend.integer("servicePort"));
    }

    @Test
    public void testPath() {
        assertEquals("/cowjumpingoverthemoon", yaml
                .yamlMapping("spec")
                .yamlSequence("rules")
                .yamlMapping(0)
                .yamlMapping("http")
                .yamlSequence("paths")
                .yamlMapping(0)
                .string("path"));
    }



}
