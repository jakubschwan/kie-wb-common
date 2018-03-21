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
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.incrementalenabler.DefaultIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;
import org.uberfire.mocks.FileSystemTestingUtils;

public class DefaultMavenCompilerTest {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMavenCompilerTest.class);
    private FileSystemTestingUtils fileSystemTestingUtils = new FileSystemTestingUtils();
    private IOService ioService;
    private Path mavenRepo;

    @Before
    public void setUp() throws Exception {
        fileSystemTestingUtils.setup();
        ioService = fileSystemTestingUtils.getIoService();

        mavenRepo = TestUtil.createMavenRepo();
    }

    @After
    public void tearDown() throws IOException {
        fileSystemTestingUtils.cleanup();
        TestUtil.rm(new File("src/../.security/"));
    }

    @Test
    public void buildWithCloneTest() throws Exception {

        final String repoName = "myrepo";
        final JGitFileSystem fs = (JGitFileSystem) ioService.newFileSystem(URI.create("git://" + repoName),
                                                                           new HashMap<String, Object>() {{
                                                                               put("init",
                                                                                   Boolean.TRUE);
                                                                               put("internal",
                                                                                   Boolean.TRUE);
                                                                           }});

        ioService.startBatch(fs);

        ioService.write(fs.getPath("/dummy/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/pom.xml").toPath())));
        ioService.write(fs.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/src/main/java/dummy/DummyA.java").toPath())));
        ioService.write(fs.getPath("/dummy/dummyB/src/main/java/dummy/DummyB.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/src/main/java/dummy/DummyB.java").toPath())));
        ioService.write(fs.getPath("/dummy/dummyA/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/pom.xml").toPath())));
        ioService.write(fs.getPath("/dummy/dummyB/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/pom.xml").toPath())));
        ioService.endBatch();

        Path tmpRootCloned = Files.createTempDirectory("cloned");

        Path tmpCloned = Files.createDirectories(Paths.get(tmpRootCloned.toString(),
                                                           "dummy"));

        final File gitClonedFolder = new File(tmpCloned.toFile(),
                                              ".clone.git");

        final Git cloned = Git.cloneRepository().setURI(fs.getGit().getRepository().getDirectory().toURI().toString()).setBare(false).setDirectory(gitClonedFolder).call();

        assertThat(cloned).isNotNull();

        //Compile the repo

        Path prjFolder = Paths.get(gitClonedFolder + "/dummy/");
        byte[] encoded = Files.readAllBytes(Paths.get(prjFolder + "/pom.xml"));
        String pomAsAstring = new String(encoded,
                                         StandardCharsets.UTF_8);
        assertThat(pomAsAstring).doesNotContain(ResourcesConstants.TAKARI_LIFECYCLE_ARTIFACT);

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.LOG_OUTPUT_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjFolder);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE);

        CompilationResponse res = compiler.compile(req);


        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithCloneTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        Path incrementalConfiguration = Paths.get(prjFolder + ResourcesConstants.TARGET_TAKARI_PLUGIN);
        assertThat(incrementalConfiguration.toFile().exists()).isTrue();

        encoded = Files.readAllBytes(Paths.get(prjFolder + "/pom.xml"));
        pomAsAstring = new String(encoded,
                                  StandardCharsets.UTF_8);
        assertThat(pomAsAstring).contains(ResourcesConstants.KIE_TAKARI_LIFECYCLE_ARTIFACT);

        TestUtil.rm(tmpRootCloned.toFile());
    }

    @Test
    public void buildWithPullRebaseUberfireTest() throws Exception {

        //Setup origin in memory
        final URI originRepo = URI.create("git://repo");
        final JGitFileSystem origin = (JGitFileSystem) ioService.newFileSystem(originRepo,
                                                                               new HashMap<String, Object>() {{
                                                                                   put("init",
                                                                                       Boolean.TRUE);
                                                                                   put("internal",
                                                                                       Boolean.TRUE);
                                                                                   put("listMode",
                                                                                       "ALL");
                                                                               }});
        ioService.startBatch(origin);

        ioService.write(origin.getPath("/dummy/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/src/main/java/dummy/DummyA.java").toPath())));
        ioService.write(origin.getPath("/dummy/dummyB/src/main/java/dummy/DummyB.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/src/main/java/dummy/DummyB.java").toPath())));
        ioService.write(origin.getPath("/dummy/dummyA/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/dummyB/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/pom.xml").toPath())));
        ioService.endBatch();

        // clone into a regularfs
        Path tmpRootCloned = Files.createTempDirectory("cloned");
        Path tmpCloned = Files.createDirectories(Paths.get(tmpRootCloned.toString(),
                                                           ".clone"));

        final Git cloned = Git.cloneRepository().setURI("git://localhost:9418/repo").setBare(false).setDirectory(tmpCloned.toFile()).call();

        assertThat(cloned).isNotNull();

        PullCommand pc = cloned.pull().setRemote("origin").setRebase(Boolean.TRUE);
        PullResult pullRes = pc.call();
        assertThat(pullRes.getRebaseResult().getStatus()).isEqualTo(RebaseResult.Status.UP_TO_DATE);// nothing changed yet

        RebaseCommand rb = cloned.rebase().setUpstream("origin/master");
        RebaseResult rbResult = rb.setPreserveMerges(true).call();
        assertThat(rbResult.getStatus().isSuccessful()).isTrue();

        //Compile the repo
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.LOG_OUTPUT_AFTER);

        byte[] encoded = Files.readAllBytes(Paths.get(tmpCloned + "/dummy/pom.xml"));
        String pomAsAstring = new String(encoded,
                                         StandardCharsets.UTF_8);
        assertThat(pomAsAstring).doesNotContain(ResourcesConstants.TAKARI_LIFECYCLE_ARTIFACT);

        Path prjFolder = Paths.get(tmpCloned + "/dummy/");

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjFolder);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE);

        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithPullRebaseUberfireTest");
        }

        assertThat(res.isSuccessful()).isTrue();

        Path incrementalConfiguration = Paths.get(prjFolder + ResourcesConstants.TARGET_TAKARI_PLUGIN);
        assertThat(incrementalConfiguration.toFile().exists()).isTrue();

        encoded = Files.readAllBytes(Paths.get(prjFolder + "/pom.xml"));
        pomAsAstring = new String(encoded,
                                  StandardCharsets.UTF_8);
        assertThat(pomAsAstring).contains(ResourcesConstants.KIE_TAKARI_LIFECYCLE_ARTIFACT);

        TestUtil.rm(tmpRootCloned.toFile());
    }

    @Test
    public void buildWithJGitDecoratorTest() throws Exception {
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE);

        String MASTER_BRANCH = "master";

        //Setup origin in memory
        final URI originRepo = URI.create("git://repo");
        final JGitFileSystem origin = (JGitFileSystem) ioService.newFileSystem(originRepo,
                                                                               new HashMap<String, Object>() {{
                                                                                   put("init",
                                                                                       Boolean.TRUE);
                                                                                   put("internal",
                                                                                       Boolean.TRUE);
                                                                                   put("listMode",
                                                                                       "ALL");
                                                                               }});
        assertThat(origin).isNotNull();

        ioService.startBatch(origin);

        ioService.write(origin.getPath("/dummy/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/src/main/java/dummy/DummyA.java").toPath())));
        ioService.write(origin.getPath("/dummy/dummyB/src/main/java/dummy/DummyB.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/src/main/java/dummy/DummyB.java").toPath())));
        ioService.write(origin.getPath("/dummy/dummyA/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/dummyB/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/pom.xml").toPath())));
        ioService.endBatch();

        RevCommit lastCommit = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());

        assertThat(lastCommit).isNotNull();

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(origin.getPath("/dummy/"));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.CLEAN, MavenCLIArgs.COMPILE},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(origin.getPath("/dummy/"), res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithJGitDecoratorTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        lastCommit = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());

        assertThat(lastCommit).isNotNull();

        ioService.write(origin.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/DummyA.java").toPath())));

        RevCommit commitBefore = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());
        assertThat(commitBefore).isNotNull();
        assertThat(lastCommit.getId().toString()).isNotEqualTo(commitBefore.getId().toString());

        //recompile
        res = compiler.compile(req);
        assertThat(res.isSuccessful()).isTrue();
    }

    //
    @Test
    public void buildWithAllDecoratorsTest() throws Exception {
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_LOG_AFTER);

        String MASTER_BRANCH = "master";

        //Setup origin in memory
        final URI originRepo = URI.create("git://repo");
        final JGitFileSystem origin = (JGitFileSystem) ioService.newFileSystem(originRepo,
                                                                               new HashMap<String, Object>() {{
                                                                                   put("init",
                                                                                       Boolean.TRUE);
                                                                                   put("internal",
                                                                                       Boolean.TRUE);
                                                                                   put("listMode",
                                                                                       "ALL");
                                                                               }});
        assertThat(origin).isNotNull();

        ioService.startBatch(origin);

        ioService.write(origin.getPath("/dummy/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/kjar-2-single-resources/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/src/main/java/org/kie/maven/plugin/test/Person.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/kjar-2-single-resources/src/main/java/org/kie/maven/plugin/test/Person.java").toPath())));
        ioService.write(origin.getPath("/dummy/src/main/resources/AllResourceTypes/simple-rules.drl"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/kjar-2-single-resources/src/main/resources/AllResourceTypes/simple-rules.drl").toPath())));
        ioService.write(origin.getPath("/dummy/src/main/resources/META-INF/kmodule.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/kjar-2-single-resources/src/main/resources/META-INF/kmodule.xml").toPath())));
        ioService.endBatch();

        RevCommit lastCommit = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());
        assertThat(lastCommit).isNotNull();

        // clone into a regularfs
        Path tmpRootCloned = Files.createTempDirectory("cloned");
        Path tmpCloned = Files.createDirectories(Paths.get(tmpRootCloned.toString(),
                                                           ".clone.git"));

        final Git cloned = Git.cloneRepository().setURI("git://localhost:9418/repo").setBare(false).setDirectory(tmpCloned.toFile()).call();

        assertThat(cloned).isNotNull();

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmpCloned + "/dummy"));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE);
        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithAllDecoratorsTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        lastCommit = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());
        assertThat(lastCommit).isNotNull();

        //change one file and commit on the origin repo
        ioService.write(origin.getPath("/dummy/src/main/java/org/kie/maven/plugin/test/Person.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/Person.java").toPath())));

        RevCommit commitBefore = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());
        assertThat(commitBefore).isNotNull();
        assertThat(lastCommit.getId().toString()).isNotEqualTo(commitBefore.getId().toString());

        //recompile
        res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithAllDecoratorsTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        TestUtil.rm(tmpRootCloned.toFile());
    }

    @Test
    public void testJDTCompiler() throws Exception {

        Path tmpRoot = Files.createTempDirectory("repo");

        //NIO creation and copy content
        Path temp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                      "dummy"));
        TestUtil.copyTree(Paths.get(ResourcesConstants.DUMMY_KIE_MULTIMODULE_UNTOUCHED_WITH_ERROR_DIR),
                          temp);
        //end NIO
        Path tmp = Paths.get(tmpRoot.toAbsolutePath().toString(),
                             "dummy");

        Path mainPom = Paths.get(tmp.toAbsolutePath().toString(),
                                 "pom.xml");
        byte[] encoded = Files.readAllBytes(Paths.get(tmp.toAbsolutePath().toString(),
                                                      "pom.xml"));
        String pomAsAstring = new String(encoded,
                                         StandardCharsets.UTF_8);
        assertThat(pomAsAstring).doesNotContain("<artifactId>kie-takari-lifecycle-plugin</artifactId>");
        assertThat(pomAsAstring).doesNotContain("<packaging>kjar</packaging>");
        assertThat(pomAsAstring).doesNotContain("<compilerId>jdt</compilerId>");
        assertThat(pomAsAstring).doesNotContain("<source>1.8</source>");
        assertThat(pomAsAstring).doesNotContain("<target>1.8</target>");

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmp);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.CLEAN, MavenCLIArgs.COMPILE, "-X"},
                                                               Boolean.FALSE);
        DefaultIncrementalCompilerEnabler enabler = new DefaultIncrementalCompilerEnabler();
        assertThat(enabler.process(req).getResult()).isTrue();

        encoded = Files.readAllBytes(Paths.get(mainPom.toString()));
        pomAsAstring = new String(encoded,
                                  StandardCharsets.UTF_8);

        assertThat(pomAsAstring).contains("<compilerId>jdt</compilerId>");
        assertThat(pomAsAstring).contains("<source>1.8</source>");
        assertThat(pomAsAstring).contains("<target>1.8</target>");

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void cleanInternalTest() throws Exception {
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE);

        String MASTER_BRANCH = "master";

        //Setup origin in memory
        final URI originRepo = URI.create("git://repo");
        final JGitFileSystem origin = (JGitFileSystem) ioService.newFileSystem(originRepo,
                                                                               new HashMap<String, Object>() {{
                                                                                   put("init",
                                                                                       Boolean.TRUE);
                                                                                   put("internal",
                                                                                       Boolean.TRUE);
                                                                                   put("listMode",
                                                                                       "ALL");
                                                                               }});
        assertThat(origin).isNotNull();

        ioService.startBatch(origin);

        ioService.write(origin.getPath("/dummy/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/src/main/java/dummy/DummyA.java").toPath())));
        ioService.write(origin.getPath("/dummy/dummyB/src/main/java/dummy/DummyB.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/src/main/java/dummy/DummyB.java").toPath())));
        ioService.write(origin.getPath("/dummy/dummyA/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/pom.xml").toPath())));
        ioService.write(origin.getPath("/dummy/dummyB/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/pom.xml").toPath())));
        ioService.endBatch();

        RevCommit lastCommit = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());

        assertThat(lastCommit).isNotNull();

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(origin.getPath("/dummy/"));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.CLEAN, MavenCLIArgs.COMPILE},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(origin.getPath("/dummy/"), res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithJGitDecoratorTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        lastCommit = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());

        assertThat(lastCommit).isNotNull();

        ioService.write(origin.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/DummyA.java").toPath())));

        RevCommit commitBefore = origin.getGit().resolveRevCommit(origin.getGit().getRef(MASTER_BRANCH).getObjectId());
        assertThat(commitBefore).isNotNull();
        assertThat(lastCommit.getId().toString()).isNotEqualTo(commitBefore.getId().toString());

        assertThat(compiler.cleanInternalCache()).isTrue();

        // recompile
        res = compiler.compile(req);
        // assert commits
        assertThat(res.isSuccessful()).isTrue();
    }
}
