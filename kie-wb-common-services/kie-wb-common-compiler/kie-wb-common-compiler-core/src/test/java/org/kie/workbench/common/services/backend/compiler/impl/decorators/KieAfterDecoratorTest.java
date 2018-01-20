package org.kie.workbench.common.services.backend.compiler.impl.decorators;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class KieAfterDecoratorTest {

    private static Path mavenRepo;
    private static Logger logger = LoggerFactory.getLogger(KieAfterDecoratorTest.class);
    private static Path tmpRoot;
    private static String alternateSettingsAbsPath;
    private static WorkspaceCompilationInfo info;

    @BeforeClass
    public static void setUp() throws Exception {
        mavenRepo = Paths.get(System.getProperty("user.home"),
                              "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }

        tmpRoot = Files.createTempDirectory("repo");
        alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"), tmp);
        info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
    }

    @AfterClass
    public static void tearDown(){
        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void compileTest(){

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
    public void compileWithOverrideTest() throws Exception{

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
