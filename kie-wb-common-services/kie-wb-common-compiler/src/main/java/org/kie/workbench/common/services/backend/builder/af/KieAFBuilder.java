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
package org.kie.workbench.common.services.backend.builder.af;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.uberfire.java.nio.file.Path;

public interface KieAFBuilder {

    /**
     * Clean internal poms cached
     */
    CompletableFuture<Boolean> cleanInternalCache();

    /**
     * Run a mvn compile and create the output and the prj dependencies in the response
     */
    CompletableFuture<KieCompilationResponse> validate(final Path path,
                                    final InputStream inputStream);

    /**
     * Run a mvn compile if is used the contructor with no []args or run the maven tasks declared in the []args passed with
     * the prj and maven repo in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder and
     * maven repo remain the same between compilaton requests
     */
    CompletableFuture<KieCompilationResponse> build();

    /**
     * Run a mvn compile and create the output and the prj dependencies in the response
     */
    CompletableFuture<KieCompilationResponse> build(Boolean logRequested, Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn package on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder and maven repo
     * remain the same between compilaton requests
     */
    CompletableFuture<KieCompilationResponse> buildAndPackage();

    /**
     * Run a mvn package on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder and maven repo
     * remain the same between compilaton requests
     */
    CompletableFuture<KieCompilationResponse> buildAndPackage(Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn install on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder
     * and maven repo remain the same between compilaton requests
     */
    CompletableFuture<KieCompilationResponse> buildAndInstall();

    /**
     * Run a mvn install on the prj and maven repo configured in the constructor, maven output provided in the
     * CompilationResponse, the internal objects in the impl will be reused, useful if the project folder
     * and maven repo remain the same between compilaton requests
     */
    CompletableFuture<KieCompilationResponse> buildAndInstall(Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn compile on the prj configured in the constructor, maven output provided in the CompilationResponse,
     * a new CompilationRequest will be created at every invocation, useful if the project folder remain the same but
     * different maven repo are required between compilation requests
     */
    CompletableFuture<KieCompilationResponse> build(String mavenRepo);

    /**
     * Run a mvn compile on the prj configured in the constructor, maven output provided in the CompilationResponse,
     * a new CompilationRequest will be created at every invocation, useful if the project folder remain the same but
     * different maven repo are required between compilation requests
     */
    CompletableFuture<KieCompilationResponse> build(String mavenRepo, Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn compile on the projectPath with mavenRepo specified, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> build(String projectPath,
                                 String mavenRepo);

    /**
     * Run a mvn compile on the projectPath with mavenRepo specified, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> build(String projectPath,
                                 String mavenRepo, Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn compile package on the projectPath, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> buildAndPackage(String projectPath,
                                           String mavenRepo);

    /**
     * Run a mvn compile package on the projectPath, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> buildAndPackage(String projectPath,
                                           String mavenRepo, Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn install on the projectPath, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath,
                                           String mavenRepo);

    /**
     * Run a mvn install on the projectPath, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath,
                                           String mavenRepo, Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder, maven repo and
     * maven args changes between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath,
                                            String mavenRepo,
                                            String[] args);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder, maven repo and
     * maven args changes between compilation Requests
     */
    CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath,
                                            String mavenRepo,
                                            String[] args, Boolean skipPrjDependenciesCreationList);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse, behaviour before and after compilation based
     * on the decoator, a new CompilationRequest will be created at every invocation
     */
    CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath,
                                            String mavenRepo,
                                            String[] args,
                                            KieDecorator decorator);

    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse, behaviour before and after compilation based
     * on the decoator, a new CompilationRequest will be created at every invocation
     */
    CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath,
                                            String mavenRepo,
                                            String[] args,
                                            KieDecorator decorator, Boolean skipPrjDependenciesCreationList);

}
