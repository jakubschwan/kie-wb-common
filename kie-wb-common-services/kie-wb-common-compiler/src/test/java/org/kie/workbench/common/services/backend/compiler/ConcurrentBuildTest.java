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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private volatile Integer counter;

    @Before
    public void setUp() throws Exception {
        mavenRepo = Paths.get(System.getProperty("user.home"), "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Test
    public void buildFourProjectsInFourThreadCompletableFuture() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            counter = 0;

            CompletableFuture<KieCompilationResponse> resOne = CompletableFuture.supplyAsync(() -> compileAndloadKieJarSingleMetadataWithPackagedJar(), executor);
            CompletableFuture<KieCompilationResponse> resTwo = CompletableFuture.supplyAsync(() -> compileAndLoadKieJarMetadataAllResourcesPackagedJar(), executor);
            CompletableFuture<KieCompilationResponse> resThree = CompletableFuture.supplyAsync(() -> compileAndloadKieJarSingleMetadataWithPackagedJar(), executor);
            CompletableFuture<KieCompilationResponse> resFour = CompletableFuture.supplyAsync(() -> compileAndLoadKieJarMetadataAllResourcesPackagedJar(), executor);

            while (counter < 4) {
            }
            logger.info("ThreadExecuted:" + counter + ", executor shutdown");
            executor.shutdownNow();
            Assert.assertTrue(resOne.get().isSuccessful());
            Assert.assertTrue(resTwo.get().isSuccessful());
            Assert.assertTrue(resThree.get().isSuccessful());
            Assert.assertTrue(resFour.get().isSuccessful());
        } catch (Exception e) {
            logger.error("tasks interrupted");
        } finally {
            if (!executor.isTerminated()) {
                logger.error("cancel non-finished tasks");
            }
            executor.shutdownNow();
            logger.info("shutdown finished");
        }
    }

    @Test
    public void buildFourProjectsInFourThread() {
        counter = 0;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<KieCompilationResponse>> tasks = Arrays.asList(
                    () -> compileAndloadKieJarSingleMetadataWithPackagedJar(),
                    () -> compileAndloadKieJarSingleMetadataWithPackagedJar(),
                    () -> compileAndLoadKieJarMetadataAllResourcesPackagedJar(),
                    () -> compileAndLoadKieJarMetadataAllResourcesPackagedJar()
            );
            List<Future<KieCompilationResponse>> results = executor.invokeAll(tasks);
            while (counter < 4) {
            }
            logger.info("ThreadExecuted:" + counter + ", executor shutdown");
            executor.shutdownNow();

            logger.info("\nFinished all threads ");
            Assert.assertTrue(results.size() == 4);
            for (Future<KieCompilationResponse> result : results) {
                logger.info("Working dir:" + result.get().getWorkingDir().get() + " success:" + result.get().isSuccessful());
            }
            for (Future<KieCompilationResponse> result : results) {
                Assert.assertTrue(result.get().isSuccessful());
            }
        } catch (ExecutionException ee) {
            logger.error(ee.getMessage());
        } catch (InterruptedException e) {
            logger.error("tasks interrupted");
        } finally {
            if (!executor.isTerminated()) {
                logger.error("cancel non-finished tasks");
            }
            executor.shutdownNow();
            logger.info("shutdown finished");
        }
    }

    @Test
    public void buildFourProjectsInTheSameThread() throws Exception {
        counter = 0;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {

            Callable<Map<Integer, KieCompilationResponse>> task1 = () -> {
                Map<Integer, KieCompilationResponse> map = new ConcurrentHashMap<>(2);
                KieCompilationResponse r1 = compileAndloadKieJarSingleMetadataWithPackagedJar();
                KieCompilationResponse r2 = compileAndLoadKieJarMetadataAllResourcesPackagedJar();
                KieCompilationResponse r3 = compileAndloadKieJarSingleMetadataWithPackagedJar();
                KieCompilationResponse r4 = compileAndLoadKieJarMetadataAllResourcesPackagedJar();
                map.put(1, r1);
                map.put(2, r2);
                map.put(3, r3);
                map.put(4, r4);
                return map;
            };

            Future<Map<Integer, KieCompilationResponse>> future = executor.submit(task1);
            while (counter < 4) {
            }
            Map<Integer, KieCompilationResponse> result = future.get();// blocking call
            Assert.assertTrue(result.get(1).isSuccessful());
            Assert.assertTrue(result.get(2).isSuccessful());
            Assert.assertTrue(result.get(3).isSuccessful());
            Assert.assertTrue(result.get(4).isSuccessful());
            logger.info("\nFinished all threads");
        } catch (InterruptedException e) {
            logger.error("tasks interrupted");
        } finally {
            if (!executor.isTerminated()) {
                logger.error("cancel non-finished tasks");
            }
            executor.shutdownNow();
            logger.info("shutdown finished");
        }
    }

    private KieCompilationResponse compileAndloadKieJarSingleMetadataWithPackagedJar() {
        String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmpRoot = Files.createTempDirectory("repo_" + UUID.randomUUID().toString());
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        try {
            TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"), tmp);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AND_LOG_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{
                                                                       MavenCLIArgs.COMPILE,
                                                                       MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                               },
                                                               Boolean.TRUE, Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compileSync(req);
        logger.info("\nFinished " + res.isSuccessful() + " Single metadata tmp:" + tmp + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
        if (!res.isSuccessful()) {
            try {
                logger.error(" Fail, writing output on target folder:" + tmp + " UUID:" + req.getRequestUUID());
                TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                          "ConcurrentBuildTest.compileAndloadKieJarSingleMetadataWithPackagedJar_" + req.getRequestUUID());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        counter++;
        return res;
    }

    private KieCompilationResponse compileAndLoadKieJarMetadataAllResourcesPackagedJar() {
        String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmpRoot = Files.createTempDirectory("repo_" + UUID.randomUUID().toString());
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        try {
            TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"), tmp);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(
                KieDecorator.KIE_AND_LOG_AFTER);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmp);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{
                                                                       MavenCLIArgs.COMPILE,
                                                                       MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                               },
                                                               Boolean.TRUE, Boolean.FALSE);
        KieCompilationResponse res = (KieCompilationResponse) compiler.compileSync(req);
        logger.info("\nFinished " + res.isSuccessful() + " all Metadata tmp:" + tmp + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
        if (!res.isSuccessful()) {
            try {
                logger.error("writing output on target folder:" + tmp);
                TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                          "ConcurrentBuildTest.compileAndLoadKieJarMetadataAllResourcesPackagedJar_" + req.getRequestUUID());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        counter++;
        return res;
    }
}
