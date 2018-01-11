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
package org.kie.workbench.common.services.backend.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;

import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.service.executors.DefaultLocalExecutor;
import org.kie.workbench.common.services.backend.service.executors.DefaultRemoteExecutor;
import org.uberfire.java.nio.file.Path;

@ApplicationScoped
public class DefaultKieCompilerService implements AFCompilerService {

    private DefaultLocalExecutor localExecutor;
    private DefaultRemoteExecutor remoteExecutor;

    public DefaultKieCompilerService(){
        localExecutor = new DefaultLocalExecutor(Executors.newCachedThreadPool());
        remoteExecutor = new DefaultRemoteExecutor(Executors.newCachedThreadPool());
    }



    /************************************ Suitable for the Local Builds ***********************************************/

    @Override
    public CompletableFuture<KieCompilationResponse> buildAsync(Path projectPath, String mavenRepo) {
        return localExecutor.buildAsync(projectPath, mavenRepo);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAsync(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return localExecutor.buildAsync(projectPath, mavenRepo, skipPrjDependenciesCreationList);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstallAsync(Path projectPath, String mavenRepo) {
        return localExecutor.buildAndInstallAsync(projectPath, mavenRepo);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstallAsync(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return localExecutor.buildAndInstallAsync(projectPath, mavenRepo, skipPrjDependenciesCreationList);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecializedAsync(Path projectPath, String mavenRepo, String[] args) {
        return localExecutor.buildSpecializedAsync(projectPath, mavenRepo, args);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecializedAsync(Path projectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        return localExecutor.buildSpecializedAsync(projectPath, mavenRepo, args, skipPrjDependenciesCreationList);
    }

    /************************************ Suitable for the REST Builds ************************************************/

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return remoteExecutor.build(projectPath, mavenRepo, skipPrjDependenciesCreationList);
    }

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo) {
        return remoteExecutor.build(projectPath, mavenRepo);
    }


    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo) {
        return remoteExecutor.buildAndInstall(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return remoteExecutor.buildAndInstall(projectPath, mavenRepo, skipPrjDependenciesCreationList);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args) {
        return remoteExecutor.buildSpecialized(projectPath, mavenRepo, args);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        return remoteExecutor.buildSpecialized(projectPath, mavenRepo, args, skipPrjDependenciesCreationList);
    }
}
