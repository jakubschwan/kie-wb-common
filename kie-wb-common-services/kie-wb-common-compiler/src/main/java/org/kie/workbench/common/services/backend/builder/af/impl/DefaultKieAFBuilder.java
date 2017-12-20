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
package org.kie.workbench.common.services.backend.builder.af.impl;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.api.Git;
import org.kie.workbench.common.services.backend.builder.af.KieAFBuilder;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.KieAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.OutputLogAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieDefaultMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.utils.JGitUtils;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;

public class DefaultKieAFBuilder implements KieAFBuilder {

    private Path originalProjectRootPath;
    private Git git;
    private AFCompiler<KieCompilationResponse> compiler;
    private WorkspaceCompilationInfo info;
    private CompilationRequest req;
    private String mavenRepo;
    private AtomicBoolean isBuilding = new AtomicBoolean(false);
    private KieCompilationResponse lastResponse = null;

    public DefaultKieAFBuilder(final Path projectRootPath,
                               final String mavenRepo) {
        final Path projectRepo;
        if (projectRootPath.getFileSystem() instanceof JGitFileSystem) {
            this.git = JGitUtils.tempClone((JGitFileSystem) projectRootPath.getFileSystem(), getFolderName());
            try {
                projectRepo = Paths.get(git.getRepository().getDirectory().getParentFile().toPath().resolve(projectRootPath.getFileName().toString()).toFile().getCanonicalFile().toPath().toUri());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            git = null;
            projectRepo = projectRootPath;
        }
        setup(projectRootPath, mavenRepo, git, projectRepo);
    }

    public DefaultKieAFBuilder(final Path projectRootPath,
                               final String mavenRepo,
                               final Git git,
                               final Path workingDir) {
        setup(projectRootPath, mavenRepo, git, workingDir);
    }

    private void setup(Path projectRootPath,
                       String mavenRepo,
                       Git git,
                       Path workingDir) {
        this.originalProjectRootPath = projectRootPath;
        this.git = git;
        this.mavenRepo = mavenRepo;
        this.compiler = new KieAfterDecorator(new OutputLogAfterDecorator(new KieDefaultMavenCompiler()));

        info = new WorkspaceCompilationInfo(workingDir);
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.PACKAGE},
                                            Boolean.TRUE, Boolean.FALSE);
    }

    private String getFolderName() {
        return UUID.randomUUID().toString();
    }


    /*******************************************************************************************************************************/

    @Override
    public CompletableFuture<Boolean> cleanInternalCache() {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> validate(Path path, InputStream inputStream) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build() {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(Boolean logRequested, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndPackage() {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndPackage(Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall() {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(String mavenRepo) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(String projectPath, String mavenRepo) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndPackage(String projectPath, String mavenRepo) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndPackage(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath, String mavenRepo) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args, KieDecorator decorator) {
        return null;
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args, KieDecorator decorator, Boolean skipPrjDependenciesCreationList) {
        return null;
    }
}
