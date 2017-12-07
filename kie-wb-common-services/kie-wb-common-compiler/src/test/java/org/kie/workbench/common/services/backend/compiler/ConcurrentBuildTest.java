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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class ConcurrentBuildTest {

    private Path mavenRepo;
    private Logger logger = LoggerFactory.getLogger(ConcurrentBuildTest.class);

    private String alternateSettingsAbsPath, alternateSettingsAbsPathTwo, alternateSettingsAbsPathThree, alternateSettingsAbsPathFour;
    private Path tmpRoot, tmpRootTwo, tmpRootThree, tmpRootFour;
    private Path tmp, tmpTwo, tmpThree, tmpFour;

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
    public void buildTwoProjectsInTheSameThread() throws Exception {
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJar();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {

            Callable<Map<Integer,KieCompilationResponse>> task1 = () -> {
                Map<Integer,KieCompilationResponse> map = new ConcurrentHashMap<>(2);
                KieCompilationResponse r1 = compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPath, tmp);
                KieCompilationResponse r2 = compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPathTwo, tmpTwo);
                map.put(1,r1);
                map.put(2,r2);
                return map;
            };

            Future<Map<Integer,KieCompilationResponse>> future = executor.submit(task1);

            Map<Integer,KieCompilationResponse> result = future.get();
            KieCompilationResponse one = result.get(1);
            KieCompilationResponse two = result.get(2);
            Assert.assertTrue(one.isSuccessful());
            Assert.assertFalse(two.isSuccessful());

            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("tasks interrupted");
        } finally {
            if (!executor.isTerminated()) {
                System.err.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
            System.out.println("shutdown finished");
        }
    }

    @Test @Ignore
    public void buildTwoProjectsInTwoThread() throws Exception {
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJar();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar();

        Runnable task1 = () -> {
            compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPath, tmp);
        };
        Runnable task2 = () -> {
            compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPathTwo, tmpTwo);
        };
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();
        while (true) {
            try {
                t1.join();
                t2.join();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void prepareCompileAndloadKieJarSingleMetadataWithPackagedJar() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        tmpRoot = Files.createTempDirectory("repo");
        tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"),
                          tmp);
    }

    private void prepareCompileAndloadKieJarSingleMetadataWithPackagedJarTwo() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        alternateSettingsAbsPathThree = new File("src/test/settings.xml").getAbsolutePath();
        tmpRootThree = Files.createTempDirectory("repo");
        tmpThree = Files.createDirectories(Paths.get(tmpRootThree.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"),
                          tmpThree);
    }

    private void prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        alternateSettingsAbsPathTwo = new File("src/test/settings.xml").getAbsolutePath();
        //compile and install
        tmpRootTwo = Files.createTempDirectory("repo");
        //NIO creation and copy content
        tmpTwo = Files.createDirectories(Paths.get(tmpRootTwo.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"),
                          tmpTwo);
        //end NIO
    }

    private void prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJarTwo() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        alternateSettingsAbsPathFour = new File("src/test/settings.xml").getAbsolutePath();
        //compile and install
        tmpRootFour = Files.createTempDirectory("repo");
        //NIO creation and copy content
        tmpFour = Files.createDirectories(Paths.get(tmpRootFour.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"),
                          tmpFour);
        //end NIO
    }

    private KieCompilationResponse compileAndloadKieJarSingleMetadataWithPackagedJar(String alternateSettingsAbsPath, Path tmp) {

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE, Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compileSync(req);
        if (!res.getMavenOutput().isEmpty() && !res.isSuccessful()) {
            try {
                TestUtil.writeMavenOutputIntoTargetFolder(res.getMavenOutput(),
                                                          "KieMetadataTest.compileAndloadKieJarSingleMetadataWithPackagedJar");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        if (!res.isSuccessful() && !res.getMavenOutput().isEmpty()) {
            List<String> msgs = res.getMavenOutput();
            for (String msg : msgs) {
                logger.info(msg);
            }
        }
        return res;
/*
        Assert.assertTrue(res.isSuccessful());

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        Assert.assertTrue(metaDataOptional.isPresent());
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        Assert.assertNotNull(kieModuleMetaInfo);

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        Assert.assertEquals(rulesBP.size(),
                            1);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        Assert.assertTrue(kieModuleOptional.isPresent());
        KieModule kModule = kieModuleOptional.get();

        Assert.assertTrue(res.getDependenciesAsURI().isEmpty());
        Assert.assertTrue(res.getDependenciesAsURI().size() == 5);

        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getDependenciesAsURI());

        //KieModuleMetaData kieModuleMetaData = KieModuleMetaData.Factory.newKieModuleMetaData(kModule); // broken
        Assert.assertNotNull(kieModuleMetaData);

        //comment if you want read the log file after the test run
        //TestUtil.rm(tmpRoot.toFile());*/
    }

    private KieCompilationResponse compileAndLoadKieJarMetadataAllResourcesPackagedJar(String alternateSettingsAbsPathTwo, Path tmpTwo) {

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(
                KieDecorator.KIE_AND_LOG_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmpTwo);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPathTwo},
                                                               Boolean.TRUE, Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compileSync(req);

        if (!res.getMavenOutput().isEmpty() && !res.isSuccessful()) {
            try {
                TestUtil.writeMavenOutputIntoTargetFolder(res.getMavenOutput(),
                                                          "KieMetadataTest.compileAndLoadKieJarMetadataAllResourcesPackagedJar");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        if (!res.isSuccessful() && !res.getMavenOutput().isEmpty()) {
            List<String> msgs = res.getMavenOutput();
            for (String msg : msgs) {
                logger.info(msg);
            }
        }
        return res;
/*
        Assert.assertTrue(res.isSuccessful());

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        Assert.assertTrue(metaDataOptional.isPresent());
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        Assert.assertNotNull(kieModuleMetaInfo);

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        Assert.assertEquals(rulesBP.size(),
                            8);
        Map<String, TypeMetaInfo> typesMI = kieModuleMetaInfo.getTypeMetaInfos();
        Assert.assertEquals(typesMI.size(),
                            35);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        Assert.assertTrue(kieModuleOptional.isPresent());

        Assert.assertTrue(!res.getDependenciesAsURI().isEmpty());
        Assert.assertTrue(res.getDependenciesAsURI().size() == 5);
        KieModule kModule = kieModuleOptional.get();

        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getDependenciesAsURI());
        Assert.assertNotNull(kieModuleMetaData);
        //comment if you want read the log file after the test run
        //TestUtil.rm(tmpRootTwo.toFile());*/
    }

    @Test @Ignore
    public void buildFourProjectsInFourThread() throws Exception {
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJar();
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJarTwo();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJarTwo();

        Runnable task1 = () -> {
            compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPath, tmp);
        };
        Runnable task2 = () -> {
            compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPathTwo, tmpTwo);
        };

        Runnable task3 = () -> {
            compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPathThree, tmpThree);
        };
        Runnable task4 = () -> {
            compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPathFour, tmpFour);
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        Thread t3 = new Thread(task3);
        Thread t4 = new Thread(task4);
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        while (true) {
            try {
                t1.join();
                t2.join();
                t3.join();
                t4.join();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
