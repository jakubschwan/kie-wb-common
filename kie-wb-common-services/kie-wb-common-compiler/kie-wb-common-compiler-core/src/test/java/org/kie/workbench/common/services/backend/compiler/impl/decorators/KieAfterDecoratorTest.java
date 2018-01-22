package org.kie.workbench.common.services.backend.compiler.impl.decorators;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.uberfire.java.nio.file.Path;

public class KieAfterDecoratorTest extends BaseCompilerTest {

    public KieAfterDecoratorTest() {
        super("target/test-classes/kjar-2-single-resources");
    }

    @Test
    public void compileTest() {

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        KieAfterDecorator decorator = new KieAfterDecorator(new BaseMavenCompiler());
        KieCompilationResponse kieRes = (KieCompilationResponse) decorator.compile(req);
        Assert.assertTrue(kieRes.isSuccessful());
        Assert.assertTrue(kieRes.getMavenOutput().size() == 0);
        Assert.assertNotNull(kieRes.getKieModule());
        Assert.assertNotNull(kieRes.getKieModuleMetaInfo());
    }

    @Test
    public void compileWithOverrideTest() throws Exception {

        Map<Path, InputStream> override = new HashMap<>();
        org.uberfire.java.nio.file.Path path = org.uberfire.java.nio.file.Paths.get(tmpRoot + "/dummy/src/main/java/org/kie/maven/plugin/test/Person.java");
        InputStream input = new FileInputStream(new File("target/test-classes/kjar-2-single-resources_override/src/main/java/org/kie/maven/plugin/test/Person.java"));
        override.put(path, input);

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        KieAfterDecorator decorator = new KieAfterDecorator(new BaseMavenCompiler());
        KieCompilationResponse kieRes = (KieCompilationResponse) decorator.compile(req, override);
        Assert.assertTrue(kieRes.isSuccessful());
        Assert.assertTrue(kieRes.getMavenOutput().size() == 0);
        Assert.assertNotNull(kieRes.getKieModule());
        Assert.assertNotNull(kieRes.getKieModuleMetaInfo());
    }
}
