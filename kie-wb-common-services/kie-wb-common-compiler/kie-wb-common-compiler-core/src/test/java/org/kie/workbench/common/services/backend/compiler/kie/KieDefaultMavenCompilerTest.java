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

package org.kie.workbench.common.services.backend.compiler.kie;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.ResourcesConstants;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;
import org.uberfire.mocks.FileSystemTestingUtils;

public class KieDefaultMavenCompilerTest {

    private static final Logger logger = LoggerFactory.getLogger(KieDefaultMavenCompilerTest.class);
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
        File sec = new File("src/../.security/");
        if (sec.exists()) {
            TestUtil.rm(sec);
        }
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
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/dummy_multimodule_untouched/pom.xml").toPath())));
        ioService.write(fs.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/dummy_multimodule_untouched/dummyA/src/main/java/dummy/DummyA.java").toPath())));
        ioService.write(fs.getPath("/dummy/dummyB/src/main/java/dummy/DummyB.java"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/dummy_multimodule_untouched/dummyB/src/main/java/dummy/DummyB.java").toPath())));
        ioService.write(fs.getPath("/dummy/dummyA/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/dummy_multimodule_untouched/dummyA/pom.xml").toPath())));
        ioService.write(fs.getPath("/dummy/dummyB/pom.xml"),
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/dummy_multimodule_untouched/dummyB/pom.xml").toPath())));
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
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);


        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerTest.buildWithCloneTest");
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
        byte[] encoded = Files.readAllBytes(Paths.get(tmpCloned + "/dummy/pom.xml"));
        String pomAsAstring = new String(encoded,
                                         StandardCharsets.UTF_8);
        assertThat(pomAsAstring).doesNotContain(ResourcesConstants.TAKARI_LIFECYCLE_ARTIFACT);

        Path prjFolder = Paths.get(tmpCloned + "/dummy/");

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.LOG_OUTPUT_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjFolder);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE);
        CompilationResponse res = compiler.compile(req);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerTest.buildWithPullRebaseUberfireTest");
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

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(origin.getPath("/dummy/"));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.CLEAN, MavenCLIArgs.COMPILE},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);


        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(origin.getPath("/dummy/"), res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerTest.buildWithJGitDecoratorTest");
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
//        assert commits
        assertThat(res.isSuccessful()).isTrue();
    }

    //
    @Test
    public void buildWithAllDecoratorsTest() throws Exception {
        String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
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

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_LOG_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmpCloned + "/dummy"));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.TRUE);
        CompilationResponse res = compiler.compile(req);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerTest.buildWithAllDecoratorsTest");
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
                                                      "KieDefaultMavenCompilerTest.buildWithAllDecoratorsTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        TestUtil.rm(tmpRootCloned.toFile());
    }

    @Test
    public void buildCompileWithOverrideOnRegularFSTest() throws Exception {

        String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmpRoot = Files.createTempDirectory("repo");
        //NIO creation and copy content
        Path temp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy"), temp);
        //end NIO


        byte[] pomOverride = Files.readAllBytes(Paths.get("src/test/projects/dummy_override/pom.xml"));
        Files.write(Paths.get(temp.toString(), "pom.xml"), pomOverride);

        byte[] encoded = Files.readAllBytes(Paths.get(temp.toString(),
                                                      "/src/main/java/dummy/Dummy.java"));
        String dummyAsAstring = new String(encoded,
                                           StandardCharsets.UTF_8);
        assertThat(dummyAsAstring).doesNotContain("public Dummy(Integer age).isFalse() {\n" +
                                                    "        this.age = age;\n" +
                                                    "    }");

        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.LOG_OUTPUT_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(temp);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.TRUE);

        CompilationResponse res = compiler.compile(req);
        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(temp, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerTest.buildCompileWithOverrideTest");
        }
        assertThat(res.isSuccessful()).isTrue();
        assertThat(new File(req.getInfo().getPrjPath() + "/target/classes/dummy/DummyOverride.class").exists()).isFalse();

        //change some files
        Map<org.uberfire.java.nio.file.Path, InputStream> override = new HashMap<>();

        org.uberfire.java.nio.file.Path path = org.uberfire.java.nio.file.Paths.get(req.getInfo().getPrjPath() + "/src/main/java/dummy/DummyOverride.java");
        InputStream input = new FileInputStream(new File(ResourcesConstants.DUMMY_OVERRIDE+"/src/main/java/dummy/DummyOverride.java"));
        override.put(path, input);

        org.uberfire.java.nio.file.Path pathTwo = org.uberfire.java.nio.file.Paths.get(req.getInfo().getPrjPath() + "/src/main/java/dummy/Dummy.java");
        InputStream inputTwo = new FileInputStream(new File(ResourcesConstants.DUMMY_OVERRIDE+"/src/main/java/dummy/Dummy.java"));
        override.put(pathTwo, inputTwo);

        //recompile
        res = compiler.compile(req, override);
        assertThat(res.isSuccessful()).isTrue();

        assertThat(new File(req.getInfo().getPrjPath() + "/target/classes/dummy/Dummy.class").exists()).isFalse();
        assertThat(new File(req.getInfo().getPrjPath() + "/target/classes/dummy/DummyOverride.class").exists()).isTrue();

        encoded = Files.readAllBytes(Paths.get(req.getInfo().getPrjPath().toString(),
                                               "/src/main/java/dummy/Dummy.java"));
        dummyAsAstring = new String(encoded, StandardCharsets.UTF_8);
        assertThat(dummyAsAstring).contains("public Dummy(String name) {\n" +
                                                   "        this.name = name;\n" +
                                                   "    }");
        TestUtil.rm(temp.toFile());
    }

    @Test
    public void buildCompileWithOverrideOnGitVFS() throws Exception {
        final String alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();

        final URI originRepo = URI.create("git://buildCompileWithOverrideOnGitVFS");
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

        ioService.write(origin.getPath("master", "/dummy/pom.xml"), //git://buildCompileWithOverrideOnGitVFS/dummy/pom.xml
                        new String(java.nio.file.Files.readAllBytes(new File(ResourcesConstants.DUMMY_OVERRIDE+"/pom.xml").toPath())));
        ioService.write(origin.getPath("master", "/dummy/src/main/java/dummy/Dummy.java"), //git://buildCompileWithOverrideOnGitVFS/dummy/src/main/java/dummy/Dummy.java
                        new String(java.nio.file.Files.readAllBytes(new File("target/test-classes/dummy/src/main/java/dummy/Dummy.java").toPath())));

        ioService.endBatch();



        byte[] encoded = ioService.readAllBytes(origin.getPath("master", "/dummy/src/main/java/dummy/Dummy.java"));

        String dummyAsAstring = new String(encoded,
                                           StandardCharsets.UTF_8);
        assertThat(dummyAsAstring).doesNotContain("public Dummy(Integer age) {\n" +
                                                    "        this.age = age;\n" +
                                                    "    }");

        final AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_LOG_AFTER);
        WorkspaceCompilationInfo info =   new WorkspaceCompilationInfo(origin.getPath("master", "/dummy/"));// git://buildCompileWithOverrideOnGitVFS/dummy/
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.TRUE);
        CompilationResponse res = compiler.compile(req);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(res.getWorkingDir().get(),
                                                      res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerTest.buildCompileWithOverrideTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        assertThat(new File(res.getWorkingDir().get() + "/target/classes/dummy/DummyOverride.class").exists()).isFalse(); ///file:///User/temp8998876986179/dummy//target/classes/dummy/DummyOverride.class

        //change some files
        Map<org.uberfire.java.nio.file.Path, InputStream> override = new HashMap<>();

        org.uberfire.java.nio.file.Path path = origin.getPath("master", "/dummy/src/main/java/dummy/DummyOverride.java");
        InputStream input = new FileInputStream(new File(ResourcesConstants.DUMMY_OVERRIDE+"/src/main/java/dummy/DummyOverride.java"));
        override.put(path, input);

        org.uberfire.java.nio.file.Path pathTwo = origin.getPath("master", "/dummy//src/main/java/dummy/Dummy.java");
        InputStream inputTwo = new FileInputStream(new File(ResourcesConstants.DUMMY_OVERRIDE+"/src/main/java/dummy/Dummy.java"));
        override.put(pathTwo, inputTwo);

        //recompile
        res = compiler.compile(req, override);
        assertThat(res.isSuccessful()).isTrue();

        assertThat(new File(res.getWorkingDir().get() + "/target/classes/dummy/Dummy.class").exists()).isFalse();
        assertThat(new File(res.getWorkingDir().get() + "/target/classes/dummy/DummyOverride.class").exists()).isTrue();

        encoded = Files.readAllBytes(Paths.get(res.getWorkingDir().get().toString(),
                                               "/src/main/java/dummy/Dummy.java"));
        dummyAsAstring = new String(encoded, StandardCharsets.UTF_8);
        assertThat(dummyAsAstring).contains("public Dummy(String name) {\n" +
                                                   "        this.name = name;\n" +
                                                   "    }");

        compiler.cleanInternalCache();
        TestUtil.rm(origin.getGit().getRepository().getDirectory());
    }
}
