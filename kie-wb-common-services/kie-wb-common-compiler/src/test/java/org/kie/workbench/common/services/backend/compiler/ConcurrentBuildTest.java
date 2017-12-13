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

    private KieCompilationResponse compileAndloadKieJarSingleMetadataWithPackagedJar() throws Exception {

        Path tmpRoot = Files.createTempDirectory("repo_" + UUID.randomUUID().toString());
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"), tmp);
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
        System.out.println("\nFinished " + res.isSuccessful() + " Single metadata tmp:" + tmp + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
        if (!res.isSuccessful()) {
            try {
                System.out.println(" Fail, writing output on target folder:" + tmp + " UUID:" + req.getRequestUUID());
                TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                          "ConcurrentBuildTest.compileAndloadKieJarSingleMetadataWithPackagedJar_" + req.getRequestUUID());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return res;
    }

    private KieCompilationResponse compileAndLoadKieJarMetadataAllResourcesPackagedJar() throws Exception {
        Path tmpRoot = Files.createTempDirectory("repo_" + UUID.randomUUID().toString());
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"), tmp);
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
        System.out.println("\nFinished " + res.isSuccessful() + " all Metadata tmp:" + tmp + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
        if (!res.isSuccessful()) {
            try {
                System.out.println("writing output on target folder:" + tmp);
                TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                          "ConcurrentBuildTest.compileAndLoadKieJarMetadataAllResourcesPackagedJar_" + req.getRequestUUID());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return res;
    }

    @Test
    public void buildFourProjectsInFourThreadAsync() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            compileAndloadKieJarSingleMetadataWithPackagedJarAsync();
        });
        executor.execute(() -> {
            compileAndLoadKieJarMetadataAllResourcesPackagedJarAsync();
        });
        executor.execute(() -> {
            compileAndloadKieJarSingleMetadataWithPackagedJarAsync();
        });
        executor.execute(() -> {
            compileAndLoadKieJarMetadataAllResourcesPackagedJarAsync();
        });

        executor.awaitTermination(4, TimeUnit.MINUTES);
        System.out.println("\nFinished all threads ");
        //WIP
    }

    @Test
    public void buildFourProjectsInFourThread() throws Exception {

        List<Callable<KieCompilationResponse>> tasks = Arrays.asList(
                () -> compileAndloadKieJarSingleMetadataWithPackagedJar(),
                () -> compileAndloadKieJarSingleMetadataWithPackagedJar(),
                () -> compileAndLoadKieJarMetadataAllResourcesPackagedJar(),
                () -> compileAndLoadKieJarMetadataAllResourcesPackagedJar()
        );

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<KieCompilationResponse>> results = executor.invokeAll(tasks);
            executor.awaitTermination(4, TimeUnit.MINUTES);
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {

            Callable<Map<Integer, KieCompilationResponse>> task1 = () -> {
                Map<Integer, KieCompilationResponse> map = new ConcurrentHashMap<>(2);
                KieCompilationResponse r1 = compileAndloadKieJarSingleMetadataWithPackagedJar();
                KieCompilationResponse r2 = compileAndLoadKieJarMetadataAllResourcesPackagedJar();
                map.put(1, r1);
                map.put(2, r2);
                return map;
            };

            Future<Map<Integer, KieCompilationResponse>> future = executor.submit(task1);
            Map<Integer, KieCompilationResponse> result = future.get();// blocking call
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

    private CompletableFuture<KieCompilationResponse> compileAndloadKieJarSingleMetadataWithPackagedJarAsync() {
        try {
            Path tmpRoot = Files.createTempDirectory("repo_" + UUID.randomUUID().toString());
            Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
            TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-single-resources"), tmp);
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
            System.out.println("\nFinished " + res.isSuccessful() + " Single metadata tmp:" + tmp + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
            if (!res.isSuccessful()) {
                try {
                    System.out.println(" Fail, writing output on target folder:" + tmp + " UUID:" + req.getRequestUUID());
                    TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                              "ConcurrentBuildTest.compileAndloadKieJarSingleMetadataWithPackagedJar_" + req.getRequestUUID());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }

            return CompletableFuture.completedFuture(res);
        } catch (Exception e) {
            CompletableFuture<KieCompilationResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private CompletableFuture<KieCompilationResponse> compileAndLoadKieJarMetadataAllResourcesPackagedJarAsync() {
        try {
            Path tmpRoot = Files.createTempDirectory("repo_" + UUID.randomUUID().toString());
            Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
            TestUtil.copyTree(Paths.get("target/test-classes/kjar-2-all-resources"), tmp);
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
            System.out.println("\nFinished " + res.isSuccessful() + " all Metadata tmp:" + tmp + " UUID:" + req.getRequestUUID() + " res.getMavenOutput().isEmpty():" + res.getMavenOutput().isEmpty());
            if (!res.isSuccessful()) {
                try {
                    System.out.println("writing output on target folder:" + tmp);
                    TestUtil.writeMavenOutputIntoTargetFolder(tmp, res.getMavenOutput(),
                                                              "ConcurrentBuildTest.compileAndLoadKieJarMetadataAllResourcesPackagedJar_" + req.getRequestUUID());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
            return CompletableFuture.completedFuture(res);
        } catch (Exception e) {
            CompletableFuture<KieCompilationResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
