package io.github.parj;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.runners.Suite.*;

@RunWith(Suite.class)
@SuiteClasses({
        TestDeployment.class,
        TestService.class,
        TestIngress.class,
        TestKubernetesDeployment.class
})
public class OrderTestSuite {
}
