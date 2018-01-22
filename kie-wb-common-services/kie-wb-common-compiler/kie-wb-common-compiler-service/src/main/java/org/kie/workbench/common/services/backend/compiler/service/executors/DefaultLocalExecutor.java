/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.backend.compiler.service.executors;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.guvnor.common.services.backend.cache.LRUCache;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.uberfire.java.nio.file.Path;

public class DefaultLocalExecutor implements CompilerExecutor {

    private ExecutorService executor;
    private LRUCache<Path, CompileInfo> compilerCacheForLocalInvocation;

    public DefaultLocalExecutor(ExecutorService executorService) {
        executor = executorService;
        compilerCacheForLocalInvocation = new LRUCache<Path, CompileInfo>() {
        };
    }

    private AFCompiler getCompiler(Path projectPath, String mavenRepo) {
        CompileInfo info = compilerCacheForLocalInvocation.getEntry(projectPath);
        if (info != null && info.getCompiler() != null) {
            return info.getCompiler();
        } else {
            return getNewCachedAFCompiler(projectPath, mavenRepo);
        }
    }

    private AFCompiler getNewCachedAFCompiler(Path projectPath, String mavenRepo) {
        CompileInfo info;
        AFCompiler compiler;
        info = setupCompileInfo(projectPath, mavenRepo);
        compilerCacheForLocalInvocation.setEntry(projectPath, info);
        compiler = info.getCompiler();
        return compiler;
    }

    private CompileInfo setupCompileInfo(Path workingDir, String mavenRepo) {
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_KIE_AND_LOG_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(workingDir);
        return new CompileInfo(compiler, info, mavenRepo);
    }

    private CompletableFuture<KieCompilationResponse> internalBuild(Path projectPath, String mavenRepo,
                                                                    boolean skipProjectDepCreation, String goal) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(projectPath);
        AFCompiler compiler = getCompiler(projectPath, mavenRepo);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{goal},
                                                               skipProjectDepCreation);
        return CompletableFuture.supplyAsync(() -> ((KieCompilationResponse) compiler.compile(req)), executor);
    }

    private CompletableFuture<KieCompilationResponse> internalBuild(Path projectPath, String mavenRepo,
                                                                    boolean skipProjectDepCreation, String[] args) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(projectPath);
        AFCompiler compiler = getCompiler(projectPath, mavenRepo);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               skipProjectDepCreation);
        return CompletableFuture.supplyAsync(() -> ((KieCompilationResponse) compiler.compile(req)), executor);
    }

    private CompletableFuture<KieCompilationResponse> internalBuild(Path projectPath, String mavenRepo,
                                                                    boolean skipProjectDepCreation, String goal, Map<Path, InputStream> override) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(projectPath);
        AFCompiler compiler = getCompiler(projectPath, mavenRepo);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{goal},
                                                               skipProjectDepCreation);
        return CompletableFuture.supplyAsync(() -> ((KieCompilationResponse) compiler.compile(req, override)), executor);
    }

    /************************************ Suitable for the Local Builds ***********************************************/
    @Override
    public CompletableFuture<KieCompilationResponse> build(Path projectPath, String mavenRepo) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE, MavenCLIArgs.COMPILE);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(Path projectPath, String mavenRepo, Map<Path, InputStream> override) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE, MavenCLIArgs.COMPILE, override);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(Path projectPath, String mavenRepo,
                                                           Boolean skipPrjDependenciesCreationList) {
        return internalBuild(projectPath, mavenRepo, skipPrjDependenciesCreationList, MavenCLIArgs.COMPILE);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(Path projectPath, String mavenRepo) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE, MavenCLIArgs.INSTALL);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(Path projectPath, String mavenRepo,
                                                                     Boolean skipPrjDependenciesCreationList) {
        return internalBuild(projectPath, mavenRepo, skipPrjDependenciesCreationList, MavenCLIArgs.INSTALL);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(Path projectPath, String mavenRepo,
                                                                      String[] args) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE,
                             args);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(Path projectPath, String mavenRepo,
                                                                      String[] args,
                                                                      Boolean skipPrjDependenciesCreationList) {
        return internalBuild(projectPath, mavenRepo, skipPrjDependenciesCreationList,
                             args);
    }
}