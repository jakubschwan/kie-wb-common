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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
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

    private String alternateSettingsAbsPath;
    private Path tmpRoot, tmpRootTwo, tmpRootThree, tmpRootFour;
    private Path tmp, tmpTwo, tmpThree, tmpFour;

    @Before
    public void setUp() throws Exception {
        mavenRepo = Paths.get(System.getProperty("user.home"), "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
        alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
    }

    private void prepareCompileAndloadKieJarSingleMetadataWithPackagedJar() throws Exception {
        tmpRoot = Files.createTempDirectory("repo");
        tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"), tmp);
    }

    private void prepareCompileAndloadKieJarSingleMetadataWithPackagedJarTwo() throws Exception {
        tmpRootThree = Files.createTempDirectory("repo");
        tmpThree = Files.createDirectories(Paths.get(tmpRootThree.toString(),"dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"), tmpThree);
    }

    private void prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar() throws Exception {
        tmpRootTwo = Files.createTempDirectory("repo");
        tmpTwo = Files.createDirectories(Paths.get(tmpRootTwo.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"), tmpTwo);
    }

    private void prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJarTwo() throws Exception {
        tmpRootFour = Files.createTempDirectory("repo");
        tmpFour = Files.createDirectories(Paths.get(tmpRootFour.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"), tmpFour);
    }

    private KieCompilationResponse compileAndloadKieJarSingleMetadataWithPackagedJar(String alternateSettingsAbsPath, Path tmpInternal) {

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmpInternal.toUri()));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{
                                                                        MavenCLIArgs.COMPILE,
                                                                        MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                               },
                                                               Boolean.TRUE, Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compileSync(req);
        System.out.println("\nFinished " + res.isSuccessful() + " Single metadata tmp:" + tmpInternal + " UUID:" + req.getRequestUUID() + "res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
        if (!res.isSuccessful()) {
            try {
                System.out.println(" Fail, writing output on target folder:"+tmpInternal);
                TestUtil.writeMavenOutputIntoTargetFolder(tmp,res.getMavenOutput(),
                                                          "ConcurrentBuildTest.compileAndloadKieJarSingleMetadataWithPackagedJar");
                List<String> msgs = res.getMavenOutput();
                for (String msg : msgs) {
                    logger.info(msg);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return res;
    }

    private KieCompilationResponse compileAndLoadKieJarMetadataAllResourcesPackagedJar(String alternateSettingsAbsPathTwo, Path tmpInternal) {

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(
                KieDecorator.KIE_AND_LOG_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmpInternal);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPathTwo},
                                                               Boolean.TRUE, Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compileSync(req);
        System.out.println("\nFinished " + res.isSuccessful() + " all Metadata tmp:" + tmpInternal + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
        if (!res.isSuccessful()) {
            try {
                System.out.println("writing output on target folder:"+tmpInternal);
                TestUtil.writeMavenOutputIntoTargetFolder(tmp,res.getMavenOutput(),
                                                          "ConcurrentBuildTest.compileAndLoadKieJarMetadataAllResourcesPackagedJar");
                List<String> msgs = res.getMavenOutput();
                for (String msg : msgs) {
                    logger.info(msg);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return res;
    }

    @Test
    public void buildFourProjectsInFourThread() throws Exception {
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJar();
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJarTwo();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJarTwo();

        List<Callable<KieCompilationResponse>> tasks = Arrays.asList(
                () -> compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPath, tmpTwo),
                () -> compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPath, tmp),
                () -> compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPath, tmpFour),
                () -> compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPath, tmpThree)
                );

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {

            List<Future<KieCompilationResponse>> results = executor.invokeAll(tasks);
            Boolean execution = executor.awaitTermination(3, TimeUnit.MINUTES);
            System.out.println("\nFinished all threads ");
            Assert.assertTrue(results.size() == 4);
            for (Future<KieCompilationResponse> result : results) {
                System.out.println("Working dir:" + result.get().getWorkingDir().get() + " success:" + result.get().isSuccessful());
            }
            for (Future<KieCompilationResponse> result : results) {
                Assert.assertTrue(result.get().isSuccessful());
            }
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

    @Test
    public void buildTwoProjectsInTheSameThread() throws Exception {
        prepareCompileAndloadKieJarSingleMetadataWithPackagedJar();
        prepareCompileAndLoadKieJarMetadataAllResourcesPackagedJar();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {

            Callable<Map<Integer, KieCompilationResponse>> task1 = () -> {
                Map<Integer, KieCompilationResponse> map = new ConcurrentHashMap<>(2);
                KieCompilationResponse r1 = compileAndloadKieJarSingleMetadataWithPackagedJar(alternateSettingsAbsPath, tmp);
                KieCompilationResponse r2 = compileAndLoadKieJarMetadataAllResourcesPackagedJar(alternateSettingsAbsPath, tmpTwo);
                map.put(1, r1);
                map.put(2, r2);
                return map;
            };

            Future<Map<Integer, KieCompilationResponse>> future = executor.submit(task1);
           // executor.awaitTermination(4, TimeUnit.MINUTES);
            while(!future.isDone()){}
            Map<Integer, KieCompilationResponse> result = future.get();
            KieCompilationResponse one = result.get(1);
            KieCompilationResponse two = result.get(2);
            Assert.assertTrue(one.isSuccessful());
            Assert.assertTrue(two.isSuccessful());
            System.out.println("\nFinished all threads");
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
}
