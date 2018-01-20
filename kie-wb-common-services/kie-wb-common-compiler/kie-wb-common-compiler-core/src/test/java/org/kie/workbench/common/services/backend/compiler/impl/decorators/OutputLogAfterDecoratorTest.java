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
package org.kie.workbench.common.services.backend.compiler.impl.decorators;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class OutputLogAfterDecoratorTest {

    private static Path mavenRepo;
    private static Logger logger = LoggerFactory.getLogger(OutputLogAfterDecoratorTest.class);
    private static Path tmpRoot;
    private static String alternateSettingsAbsPath;
    private static  WorkspaceCompilationInfo info;

    @BeforeClass
    public static void setUp() throws Exception {
        mavenRepo = Paths.get(System.getProperty("user.home"),
                              "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }

        tmpRoot = Files.createTempDirectory("repo");
        alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("target/test-classes/dummy"), tmp);
        info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
    }

    @AfterClass
    public static void tearDown(){
        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void compileTest(){

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        OutputLogAfterDecorator decorator = new OutputLogAfterDecorator(new BaseMavenCompiler());
        CompilationResponse res = decorator.compile(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertTrue(res.getMavenOutput().size() > 0);
    }

    @Test
    public void compileWithOverrideTest() throws Exception{

        Map<Path, InputStream> override = new HashMap<>();
        org.uberfire.java.nio.file.Path path = org.uberfire.java.nio.file.Paths.get(tmpRoot + "/dummy/src/main/java/dummy/DummyOverride.java");
        InputStream input = new FileInputStream(new File("target/test-classes/dummy_override/src/main/java/dummy/DummyOverride.java"));
        override.put(path, input);

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        OutputLogAfterDecorator decorator = new OutputLogAfterDecorator(new BaseMavenCompiler());
        CompilationResponse res = decorator.compile(req, override);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertTrue(res.getMavenOutput().size() > 0);
    }
}
