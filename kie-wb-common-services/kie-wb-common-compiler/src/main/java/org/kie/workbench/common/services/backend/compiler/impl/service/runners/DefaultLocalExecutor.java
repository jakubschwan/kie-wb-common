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
package org.kie.workbench.common.services.backend.compiler.impl.service.runners;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.kie.workbench.common.services.backend.builder.af.KieAFBuilder;
import org.kie.workbench.common.services.backend.builder.af.impl.DefaultKieAFBuilder;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class DefaultLocalExecutor implements CompilerExecutor {

    private ExecutorService executor ;

    public DefaultLocalExecutor(ExecutorService executorService){
        executor = executorService;
    }

    private KieAFBuilder getBuilder(String projectPath, String mavenRepo){
        //@TODO use a cache here
        return new DefaultKieAFBuilder(Paths.get(projectPath), mavenRepo);
    }

    private KieAFBuilder getBuilder(Path projectPath, String mavenRepo){
        //@TODO use a cache here
        return new DefaultKieAFBuilder(projectPath, mavenRepo);
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


    /************************************ Suitable for the REST Builds ************************************************/

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo) {
        return getBuilder(projectPath, mavenRepo).build(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return getBuilder(projectPath, mavenRepo).build(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo) {
        return getBuilder(projectPath, mavenRepo).buildAndInstall(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo,
                                                  Boolean skipPrjDependenciesCreationList) {
        return getBuilder(projectPath, mavenRepo).buildAndInstall(projectPath, mavenRepo,skipPrjDependenciesCreationList);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args) {
        return getBuilder(projectPath, mavenRepo).buildSpecialized(projectPath, mavenRepo, args);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args,
                                                   Boolean skipPrjDependenciesCreationList) {
        return getBuilder(projectPath, mavenRepo).buildSpecialized(projectPath, mavenRepo, args, skipPrjDependenciesCreationList);
    }
}
