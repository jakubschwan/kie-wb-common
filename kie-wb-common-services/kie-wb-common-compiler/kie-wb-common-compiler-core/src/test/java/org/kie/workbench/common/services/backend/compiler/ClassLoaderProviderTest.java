/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.backend.compiler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.core.rule.KieModuleMetaInfo;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.builder.KieModule;
import org.kie.scanner.KieModuleMetaData;
import org.kie.scanner.KieModuleMetaDataImpl;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.classloader.CompilerClassloaderUtils;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.kie.workbench.common.services.backend.compiler.impl.utils.MavenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

import static org.assertj.core.api.Assertions.fail;

public class ClassLoaderProviderTest {

    private Path mavenRepo;
    private Logger logger = LoggerFactory.getLogger(ClassLoaderProviderTest.class);

    @Before
    public void setUp() throws Exception {
        mavenRepo = Paths.get(System.getProperty("user.home"),
                              "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Test
    public void loadProjectClassloaderTest() throws Exception {
        //we use NIO for this part of the test because Uberfire lack the implementation to copy a tree
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy_kie_multimodule_classloader"),
                          tmp);

        Path uberfireTmp = Paths.get(tmp.toAbsolutePath().toString());

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.NONE);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(uberfireTmp);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.CLEAN, MavenCLIArgs.COMPILE, MavenCLIArgs.INSTALL},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                      "ClassLoaderProviderTest.loadProjectClassloaderTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        List<String> pomList = MavenUtils.searchPoms(Paths.get("src/test/projects/dummy_kie_multimodule_classloader/"));
        Optional<ClassLoader> clazzLoader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(pomList,
                                                                                                            mavenRepo.toAbsolutePath().toString());
        assertThat(clazzLoader).isNotNull();
        assertThat(clazzLoader).isPresent();
        ClassLoader prjClassloader = clazzLoader.get();

        //we try to load the only dep in the prj with a simple call method to see if is loaded or not
        Class clazz;
        try {
            clazz = prjClassloader.loadClass("org.slf4j.LoggerFactory");
            assertThat(clazz.isInterface()).isFalse();

            Method m = clazz.getMethod("getLogger",
                                       String.class);
            Logger logger = (Logger) m.invoke(clazz,
                                              "Dummy");
            assertThat(logger.getName()).isEqualTo("Dummy");
            logger.info("dependency loaded from the prj classpath");
        } catch (ClassNotFoundException e) {
            fail("Test fail due ClassNotFoundException.", e);
        }

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void loadProjectClassloaderFromStringTest() throws Exception {
        //we use NIO for this part of the test because Uberfire lack the implementation to copy a tree
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy_kie_multimodule_classloader"),
                          tmp);

        Path uberfireTmp = Paths.get(tmp.toAbsolutePath().toString());

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.NONE);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(uberfireTmp);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.CLEAN, MavenCLIArgs.COMPILE, MavenCLIArgs.INSTALL},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                      "ClassLoaderProviderTest.loadProjectClassloaderFromStringTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        Optional<ClassLoader> clazzLoader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(uberfireTmp.toAbsolutePath().toString(),
                                                                                                            mavenRepo.toAbsolutePath().toString());
        assertThat(clazzLoader).isNotNull();
        assertThat(clazzLoader.isPresent()).isTrue();
        ClassLoader prjClassloader = clazzLoader.get();

        //we try to load the only dep in the prj with a simple call method to see if is loaded or not
        Class clazz;
        try {
            clazz = prjClassloader.loadClass("org.slf4j.LoggerFactory");
            assertThat(clazz.isInterface()).isFalse();

            Method m = clazz.getMethod("getLogger",
                                       String.class);
            Logger logger = (Logger) m.invoke(clazz,
                                              "Dummy");
            assertThat(logger.getName()).isEqualTo("Dummy");
            logger.info("dependency loaded from the prj classpath");
        } catch (ClassNotFoundException e) {
            fail("Test fail due ClassNotFoundException.", e);
        }

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void loadTargetFolderClassloaderTest() throws Exception {
        //we use NIO for this part of the test because Uberfire lack the implementation to copy a tree
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy_kie_multimodule_classloader"),
                          tmp);

        Path uberfireTmp = Paths.get(tmp.toAbsolutePath().toString());

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.NONE);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(uberfireTmp);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                      "ClassLoaderProviderTest.loadTargetFolderClassloaderTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        List<String> pomList = MavenUtils.searchPoms(uberfireTmp);
        Optional<ClassLoader> clazzLoader = CompilerClassloaderUtils.getClassloaderFromProjectTargets(pomList);
        assertThat(clazzLoader).isNotNull();
        assertThat(clazzLoader.isPresent()).isTrue();
        ClassLoader prjClassloader = clazzLoader.get();

        //we try to load the only dep in the prj with a simple call method to see if is loaded or not
        Class clazz;
        try {
            clazz = prjClassloader.loadClass("dummy.DummyB");
            assertThat(clazz.isInterface()).isFalse();
            Object obj = clazz.newInstance();

            assertThat(obj.toString()).startsWith("dummy.DummyB");

            Method m = clazz.getMethod("greetings",
                                       new Class[]{});
            Object greeting = m.invoke(obj,
                                       new Object[]{});
            assertThat(greeting.toString()).isEqualTo("Hello World !");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("Test fail due ClassNotFoundException.", e);
        }

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void getClassloaderFromAllDependenciesTestSimple() {
        Path path = Paths.get(".").resolve("src/test/projects/dummy_deps_simple");
        Optional<ClassLoader> classloaderOptional = CompilerClassloaderUtils.getClassloaderFromAllDependencies(path.toAbsolutePath().toString(),
                                                                                                               mavenRepo.toAbsolutePath().toString());
        assertThat(classloaderOptional.isPresent()).isTrue();
        ClassLoader classloader = classloaderOptional.get();
        URLClassLoader urlsc = (URLClassLoader) classloader;
        assertThat(urlsc.getURLs()).hasSize(4);
    }

    @Test
    public void getClassloaderFromAllDependenciesTestComplex() {
        Path path = Paths.get(".").resolve("src/test/projects/dummy_deps_complex");
        Optional<ClassLoader> classloaderOptional = CompilerClassloaderUtils.getClassloaderFromAllDependencies(path.toAbsolutePath().toString(),
                                                                                                               mavenRepo.toAbsolutePath().toString());
        assertThat(classloaderOptional.isPresent()).isTrue();
        ClassLoader classloader = classloaderOptional.get();
        URLClassLoader urlsc = (URLClassLoader) classloader;
        assertThat(urlsc.getURLs()).hasSize(7);
    }

    @Test
    public void getResourcesFromADroolsPRJ() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"),
                          tmp);

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AND_CLASSPATH_AFTER_DEPS);
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
        if (!res.isSuccessful()) {
            List<String> msgs = res.getMavenOutput();
            for (String msg : msgs) {
                logger.info(msg);
            }
        }

        assertThat(res.isSuccessful()).isTrue();

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        assertThat(metaDataOptional.isPresent()).isTrue();
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        assertThat(kieModuleMetaInfo).isNotNull();

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        assertThat(rulesBP).hasSize(1);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        assertThat(kieModuleOptional.isPresent()).isTrue();
        KieModule kModule = kieModuleOptional.get();

        assertThat(res.getDependenciesAsURI()).hasSize(4);

        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getDependenciesAsURI());

        assertThat(kieModuleMetaData).isNotNull();

        List<String> resources = CompilerClassloaderUtils.getStringFromTargets(tmpRoot);
        assertThat(resources).hasSize(3);
        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void getResourcesFromADroolsPRJWithError() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources_with_error"),
                          tmp);

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL},
                                                               Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compile(req);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                      "KieMetadataTest.getResourcesFromADroolsPRJWithError");
        }
        if (!res.isSuccessful()) {
            List<String> msgs = res.getMavenOutput();
            for (String msg : msgs) {
                logger.info(msg);
            }
        }

        assertThat(res.isSuccessful()).isTrue();

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        assertThat(metaDataOptional.isPresent()).isTrue();
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        assertThat(kieModuleMetaInfo).isNotNull();

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        assertThat(rulesBP).hasSize(1);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        assertThat(kieModuleOptional.isPresent()).isTrue();

        List<String> classloaderOptional = CompilerClassloaderUtils.getStringFromTargets(tmpRoot);
        assertThat(classloaderOptional).hasSize(3);
        TestUtil.rm(tmpRoot.toFile());
    }
}
