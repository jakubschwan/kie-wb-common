package org.kie.workbench.common.services.backend.compiler.impl.classloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.ClassLoaderProviderTest;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.kie.workbench.common.services.backend.compiler.impl.utils.MavenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class CompilerClassloaderUtilsTest {

    private static Path mavenRepo;
    private static Logger logger = LoggerFactory.getLogger(ClassLoaderProviderTest.class);
    private static Path tmpRoot;

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
        String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"),
                          tmp);

        List<String> resources = CompilerClassloaderUtils.getStringFromTargets(tmpRoot);
        Assert.assertTrue(resources.size() == 0);

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                      "KieMetadataTest.compileAndloadKieJarSingleMetadataWithPackagedJar");
        }
    }

    @AfterClass
    public static void tearDown(){
        TestUtil.rm(tmpRoot.toFile());
    }


    @Test
    public void getStringFromTargets() {
        List<String> resources  = CompilerClassloaderUtils.getStringFromTargets(tmpRoot);
        Assert.assertTrue(resources.size() == 3);
    }


    @Test
    public void getStringsFromAllDependencies() {
        List<String> resources = CompilerClassloaderUtils.getStringsFromAllDependencies(tmpRoot);
        Assert.assertTrue(resources.size() == 1);
    }


    @Test
    public void filterClassesByPackage() {
        List<String> targets = new ArrayList<>(3);
        targets.add("/target/classes/org/kie/test/A.class");
        targets.add("/target/classes/io/akka/test/C.class");
        targets.add("/target/classes/com/acme/test/D.class");

        List<String>orgKie  = CompilerClassloaderUtils.filterClassesByPackage(targets, "org.kie");
        Assert.assertTrue(orgKie.size()==1);

        List<String> akkaTest  = CompilerClassloaderUtils.filterClassesByPackage(targets, "akka.test");
        Assert.assertTrue(akkaTest.size()==1);

        List<String> com  = CompilerClassloaderUtils.filterClassesByPackage(targets, "com");
        Assert.assertTrue(com.size()==1);

        List<String> it  = CompilerClassloaderUtils.filterClassesByPackage(targets, "it");
        Assert.assertTrue(it.size()==0);
    }

    @Test
    public void filterPathClasses() {
        List<String> targets = new ArrayList<>(3);
        targets.add("/target/classes/org/kie/test/A.class");
        targets.add("/target/classes/io/akka/test/C.class");
        targets.add("/target/classes/com/acme/test/D.class");
        targets.add("/target/classes/com/acme/test/D.class");
        targets.add(mavenRepo.toAbsolutePath().toString() + "/junit/junit/4.12/junit.jar");
        targets.add(mavenRepo.toAbsolutePath().toString() +"/junit/junit/4.12/junit-4.12.jar");

        Set<String> orgKie  = CompilerClassloaderUtils.filterPathClasses(targets, mavenRepo.toString());
        Assert.assertTrue(orgKie.size() == 4);
    }


    @Test
    public void loadDependenciesClassloaderFromProject() {
        Optional<ClassLoader> classloader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(tmpRoot.toString(), mavenRepo.toString());
        Assert.assertTrue(classloader.isPresent());
    }


    @Test
    public void loadDependenciesClassloaderFromProjectWithPomList() {
        List<String> pomList = MavenUtils.searchPoms(tmpRoot);
        Assert.assertTrue(pomList.size() == 1);
        Optional<ClassLoader> classloader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(pomList, mavenRepo.toString());
        Assert.assertTrue(classloader.isPresent());
    }

    @Test
    public void getClassloaderFromProjectTargets() {
        List<String> pomList = MavenUtils.searchPoms(tmpRoot);
        Optional<ClassLoader> classLoader  = CompilerClassloaderUtils.getClassloaderFromProjectTargets(pomList);
        Assert.assertTrue(classLoader.isPresent());
    }

    @Test
    public void getClassloaderFromAllDependencies() {
        Optional<ClassLoader> classLoader  = CompilerClassloaderUtils.getClassloaderFromAllDependencies(tmpRoot.toString()+"/dummy",mavenRepo. toString());
        Assert.assertTrue(classLoader.isPresent());
    }

    @Test
    public void createClassloaderFromCpFiles() {
        String folderPath = Paths.get("src/test/projects/dummy_cp_files").toAbsolutePath().toString();
        Optional<ClassLoader> classLoader  = CompilerClassloaderUtils.createClassloaderFromCpFiles(folderPath);
        Assert.assertTrue(classLoader.isPresent());
    }

}
