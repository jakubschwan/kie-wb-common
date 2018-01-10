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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kie.workbench.common.services.backend.builder.af.KieAFBuilder;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.KieAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.OutputLogAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.utils.JGitUtils;
import org.kie.workbench.common.services.backend.compiler.impl.utils.PathConverter;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;

public class DefaultKieAFBuilder implements KieAFBuilder {

    private Path originalProjectRootPath;
    private Git git;
    private AFCompiler<KieCompilationResponse> compiler;
    private WorkspaceCompilationInfo info;
    private String mavenRepo;

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


    public DefaultKieAFBuilder(final String projectRoot,
                               final String mavenRepo) {
        final Path projectRepo;
        final Path projectRootPath = Paths.get(projectRoot);
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
        this.compiler = new KieAfterDecorator(new OutputLogAfterDecorator(new BaseMavenCompiler()));

        info = new WorkspaceCompilationInfo(workingDir);
    }

    private String getFolderName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Boolean cleanInternalCache() {
        return compiler.cleanInternalCache();
    }

    public AFCompiler getCompiler() {
        return compiler;
    }

    public WorkspaceCompilationInfo getInfo() {
        return info;
    }

    public String getMavenRepo() {
        return mavenRepo;
    }

    public Path getGITURI() {
        return originalProjectRootPath;
    }

    public Git getGit() {
        return git;
    }

    private void gitPullAndRebase() {
        if (git != null) {
            JGitUtils.pullAndRebase(git);
        }
    }

    private KieCompilationResponse internalBuild(String mavenRepo, WorkspaceCompilationInfo info,
                                                 boolean skipProjectDepCreation, String goal) {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{goal},
                                                               skipProjectDepCreation);
        return compiler.compile(req);
    }

    private KieCompilationResponse internalBuild(String mavenRepo, WorkspaceCompilationInfo info,
                                                 boolean skipProjectDepCreation, String[] args) {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               skipProjectDepCreation);
        return compiler.compile(req);
    }

    private KieCompilationResponse doBuild(final Boolean skipPrjDependenciesCreationList) {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               skipPrjDependenciesCreationList);
        return compiler.compile(req);
    }

    /******************************************************************************************************************/

    //@TODO replace and remove with org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler.compile(org.kie.workbench.common.services.backend.compiler.CompilationRequest, java.util.Map<org.uberfire.java.nio.file.Path,java.io.InputStream>)
    @Override
    public KieCompilationResponse validate(final Path path, final InputStream inputStream) {
        if (path.getFileSystem() instanceof JGitFileSystem) {
            final java.nio.file.Path convertedToCheckedPath = git.getRepository().getDirectory().toPath().getParent().resolve(path.toString().substring(1));

            try {
                Files.copy(inputStream, convertedToCheckedPath, StandardCopyOption.REPLACE_EXISTING);
                return doBuild(Boolean.FALSE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    git.reset().setMode(ResetCommand.ResetType.HARD).call();
                } catch (GitAPIException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        java.nio.file.Path _path = java.nio.file.Paths.get(path.toUri());
        final byte[] content;
        try {
            content = Files.readAllBytes(_path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.copy(inputStream, _path, StandardCopyOption.REPLACE_EXISTING);
            return doBuild(Boolean.FALSE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Files.write(_path, content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public KieCompilationResponse build() {
        gitPullAndRebase();
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.FALSE);
        return compiler.compile(req);
    }

    @Override
    public KieCompilationResponse build(final Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return doBuild(skipPrjDependenciesCreationList);
    }

    @Override
    public KieCompilationResponse build(String mavenRepo) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, info, Boolean.FALSE, MavenCLIArgs.COMPILE);
    }

    @Override
    public KieCompilationResponse build(String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, info, skipPrjDependenciesCreationList, MavenCLIArgs.COMPILE);
    }

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, new WorkspaceCompilationInfo(Paths.get(projectPath)),
                             Boolean.FALSE, MavenCLIArgs.COMPILE);
    }

    @Override
    public KieCompilationResponse build(Path projectPath, String mavenRepo) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, new WorkspaceCompilationInfo(projectPath), Boolean.FALSE,
                             MavenCLIArgs.COMPILE);
    }

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, new WorkspaceCompilationInfo(Paths.get(projectPath)),
                             skipPrjDependenciesCreationList, MavenCLIArgs.COMPILE);
    }

    @Override
    public KieCompilationResponse build(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, new WorkspaceCompilationInfo(projectPath),
                             skipPrjDependenciesCreationList, MavenCLIArgs.COMPILE);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, new WorkspaceCompilationInfo(PathConverter.createPathFromString(projectPath)),
                             Boolean.FALSE, MavenCLIArgs.INSTALL);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo,
                                                  Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return internalBuild(mavenRepo, new WorkspaceCompilationInfo(PathConverter.createPathFromString(projectPath)),
                             skipPrjDependenciesCreationList, MavenCLIArgs.INSTALL);
    }

    @Override
    public KieCompilationResponse buildAndInstall(Path projectPath, String mavenRepo) {
        return null;
    }

    @Override
    public KieCompilationResponse buildAndInstall(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return null;
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath,
                                                   String mavenRepo,
                                                   String[] args) {
        gitPullAndRebase();
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(PathConverter.createPathFromString(projectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               Boolean.FALSE);
        return compiler.compile(req);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath,
                                                   String mavenRepo,
                                                   String[] args, Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return internalBuild(mavenRepo,
                             new WorkspaceCompilationInfo(PathConverter.createPathFromString(projectPath)),
                             skipPrjDependenciesCreationList,
                             args);
    }

    @Override
    public KieCompilationResponse buildSpecialized(Path projectPath, String mavenRepo, String[] args) {
        gitPullAndRebase();
        return internalBuild(mavenRepo,
                             new WorkspaceCompilationInfo(projectPath),
                             Boolean.FALSE,
                             args);
    }

    @Override
    public KieCompilationResponse buildSpecialized(Path projectPath, String mavenRepo, String[] args,
                                                   Boolean skipPrjDependenciesCreationList) {
        gitPullAndRebase();
        return internalBuild(mavenRepo,
                             new WorkspaceCompilationInfo(projectPath),
                             skipPrjDependenciesCreationList,
                             args);
    }
}
