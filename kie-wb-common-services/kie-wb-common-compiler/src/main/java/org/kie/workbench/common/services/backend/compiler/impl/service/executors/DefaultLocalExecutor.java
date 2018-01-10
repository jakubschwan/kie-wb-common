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
package org.kie.workbench.common.services.backend.compiler.impl.service.executors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.guvnor.common.services.backend.cache.LRUCache;
import org.kie.workbench.common.services.backend.builder.af.KieAFBuilder;
import org.kie.workbench.common.services.backend.builder.af.impl.DefaultKieAFBuilder;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.service.executors.CompilerExecutor;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class DefaultLocalExecutor implements CompilerExecutor {

    private ExecutorService executor ;
    private LRUCache<Path, KieAFBuilder> cacheForLocalInvocation;


    public DefaultLocalExecutor(ExecutorService executorService){
        executor = executorService;
        cacheForLocalInvocation = new LRUCache<Path, KieAFBuilder>(){};
    }

    private KieAFBuilder getBuilder(Path projectPath, String mavenRepo){
        KieAFBuilder builder = cacheForLocalInvocation.getEntry(projectPath);
        if(builder == null){
            builder = new DefaultKieAFBuilder(projectPath, mavenRepo);
            cacheForLocalInvocation.setEntry(projectPath, builder);
        }
        return builder;
    }



    /************************************ Suitable for the Local Builds ***********************************************/
    @Override
    public CompletableFuture<KieCompilationResponse> buildAsync(Path projectPath, String mavenRepo) {
        return CompletableFuture.supplyAsync(()-> getBuilder(projectPath, mavenRepo).build(projectPath, mavenRepo),
                                             executor);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAsync(Path projectPath, String mavenRepo,
                                                                Boolean skipPrjDependenciesCreationList) {
        return CompletableFuture.supplyAsync(()-> getBuilder(projectPath, mavenRepo).build(projectPath, mavenRepo, skipPrjDependenciesCreationList), executor);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstallAsync(Path projectPath, String mavenRepo) {
        return CompletableFuture.supplyAsync(()-> getBuilder(projectPath, mavenRepo).buildAndInstall(projectPath,
                                                                                                     mavenRepo), executor);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstallAsync(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return CompletableFuture.supplyAsync(()-> getBuilder(projectPath, mavenRepo).buildAndInstall(projectPath, mavenRepo, skipPrjDependenciesCreationList), executor);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecializedAsync(Path projectPath, String mavenRepo, String[] args) {
        return CompletableFuture.supplyAsync(()-> getBuilder(projectPath, mavenRepo).buildSpecialized(projectPath, mavenRepo, args), executor);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecializedAsync(Path projectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        return CompletableFuture.supplyAsync(()-> getBuilder(projectPath, mavenRepo).buildSpecialized(projectPath, mavenRepo, args, skipPrjDependenciesCreationList), executor);
    }

}
