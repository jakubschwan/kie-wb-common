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
package org.kie.workbench.common.services.backend.builder.af.async;

import java.util.concurrent.CompletableFuture;

import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.Decorator;

public interface AFBuilderAsync {

    /**
     * Clean internal poms cached
     */
    CompletableFuture<Boolean> cleanInternalCache();

    /**
     * Run a mvn compile if is used the contructor with no []args or run the maven tasks declared in the []args passed with
     * the prj and maven repo in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder and
     * maven repo remain the same between compilaton requests
     */
    CompletableFuture<CompilationResponse> buildAsync();

    /**
     * Run a mvn package on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder and maven repo
     * remain the same between compilaton requests
     */
    CompletableFuture<CompilationResponse> buildAndPackageAsync();

    /**
     * Run a mvn package on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder and maven repo
     * remain the same between compilaton requests
     */
    CompletableFuture<CompilationResponse> buildAndPackageAsync(Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn install on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder
     * and maven repo remain the same between compilaton requests
     */
    CompletableFuture<CompilationResponse> buildAndInstallAsync();

    /**
     * Run a mvn install on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder
     * and maven repo remain the same between compilaton requests
     */
    CompletableFuture<CompilationResponse> buildAndInstallAsync(Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn compile on the prj configured in the constructor, maven output provided in the CompilationResponse,
     * useful if the project folder remain the same but
     * different maven repo are required between compilation requests
     */
    CompletableFuture<CompilationResponse> buildAsync(String mavenRepo);

    /**
     * Run a mvn compile on the prj configured in the constructor, maven output provided in the CompilationResponse,
     * useful if the project folder remain the same but
     * different maven repo are required between compilation requests
     */
    CompletableFuture<CompilationResponse> buildAsync(String mavenRepo,
                              Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn compile on the projectPath with mavenRepo specified, maven output provided in the CompilationResponse,
     * useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildAsync(String projectPath,
                              String mavenRepo,
                              Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn compile on the projectPath with mavenRepo specified, maven output provided in the CompilationResponse,
     * useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildAsync(String projectPath,
                              String mavenRepo);

    /**
     * Run a mvn compile package on the projectPath, maven output provided in the CompilationResponse,
     * useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildAndPackageAsync(String projectPath,
                                        String mavenRepo);

    /**
     * Run a mvn compile package on the projectPath, maven output provided in the CompilationResponse,
     * useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildAndPackageAsync(String projectPath,
                                        String mavenRepo,
                                        Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn install on the projectPath, maven output provided in the CompilationResponse,
     * useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildAndInstallAsync(String kieProjectPath,
                                        String mavenRepo,
                                        Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn install on the projectPath, maven output provided in the CompilationResponse,
     * useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildAndInstallAsync(String kieProjectPath,
                                        String mavenRepo);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse,
     * useful if the project folder, maven repo and
     * maven args changes between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath,
                                         String mavenRepo,
                                         String[] args);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse,
     * useful if the project folder, maven repo and
     * maven args changes between compilation Requests
     */
    CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath,
                                         String mavenRepo,
                                         String[] args,
                                         Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse, behaviour before and after compilation based
     * on the decoator
     */
    CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath,
                                         String mavenRepo,
                                         String[] args,
                                         Decorator decorator);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse, behaviour before and after compilation based
     * on the decoator
     */
    CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath,
                                         String mavenRepo,
                                         String[] args,
                                         Decorator decorator, Boolean skipPrjDependenciesCreationList);

}