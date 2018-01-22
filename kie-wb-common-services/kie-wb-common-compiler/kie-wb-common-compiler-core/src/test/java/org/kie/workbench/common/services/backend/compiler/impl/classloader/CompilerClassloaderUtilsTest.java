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
package org.kie.workbench.common.services.backend.compiler.impl.classloader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.utils.MavenUtils;

public class CompilerClassloaderUtilsTest extends BaseCompilerTest {

    public CompilerClassloaderUtilsTest() {
        super("target/test-classes/kjar-2-single-resources", KieDecorator.KIE_AFTER);
    }

    @Test
    public void getStringFromTargets() {
        List<String> resources = CompilerClassloaderUtils.getStringFromTargets(tmpRoot);
        Assert.assertTrue(resources.size() == 3);
    }

    @Test
    public void getStringsFromAllDependencies() {
        List<String> resources = CompilerClassloaderUtils.getStringsFromAllDependencies(tmpRoot);
        Assert.assertTrue(resources.size() == 1);
    }

    @Test
    public void filterClassesByPackage() {
        List<String> targets = new ArrayList<>(3);
        targets.add("/target/classes/org/kie/test/A.class");
        targets.add("/target/classes/io/akka/test/C.class");
        targets.add("/target/classes/com/acme/test/D.class");

        List<String> orgKie = CompilerClassloaderUtils.filterClassesByPackage(targets, "org.kie");
        Assert.assertTrue(orgKie.size() == 1);

        List<String> akkaTest = CompilerClassloaderUtils.filterClassesByPackage(targets, "akka.test");
        Assert.assertTrue(akkaTest.size() == 1);

        List<String> com = CompilerClassloaderUtils.filterClassesByPackage(targets, "com");
        Assert.assertTrue(com.size() == 1);

        List<String> it = CompilerClassloaderUtils.filterClassesByPackage(targets, "it");
        Assert.assertTrue(it.size() == 0);
    }

    @Test
    public void filterPathClasses() {
        List<String> targets = new ArrayList<>(3);
        targets.add("/target/classes/org/kie/test/A.class");
        targets.add("/target/classes/io/akka/test/C.class");
        targets.add("/target/classes/com/acme/test/D.class");
        targets.add("/target/classes/com/acme/test/D.class");
        targets.add(mavenRepo.toAbsolutePath().toString() + "/junit/junit/4.12/junit.jar");
        targets.add(mavenRepo.toAbsolutePath().toString() + "/junit/junit/4.12/junit-4.12.jar");

        Set<String> orgKie = CompilerClassloaderUtils.filterPathClasses(targets, mavenRepo.toString());
        Assert.assertTrue(orgKie.size() == 4);
    }

    @Test
    public void loadDependenciesClassloaderFromProject() {
        Optional<ClassLoader> classloader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(tmpRoot.toString(), mavenRepo.toString());
        Assert.assertTrue(classloader.isPresent());
    }

    @Test
    public void loadDependenciesClassloaderFromProjectWithPomList() {
        List<String> pomList = MavenUtils.searchPoms(tmpRoot);
        Assert.assertTrue(pomList.size() == 1);
        Optional<ClassLoader> classloader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(pomList, mavenRepo.toString());
        Assert.assertTrue(classloader.isPresent());
    }

    @Test
    public void getClassloaderFromProjectTargets() {
        List<String> pomList = MavenUtils.searchPoms(tmpRoot);
        Optional<ClassLoader> classLoader = CompilerClassloaderUtils.getClassloaderFromProjectTargets(pomList);
        Assert.assertTrue(classLoader.isPresent());
    }

    @Test
    public void getClassloaderFromAllDependencies() {
        Optional<ClassLoader> classLoader = CompilerClassloaderUtils.getClassloaderFromAllDependencies(tmpRoot.toString() + "/dummy", mavenRepo.toString());
        Assert.assertTrue(classLoader.isPresent());
    }

    @Test
    public void createClassloaderFromCpFiles() {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);
        compiler.compile(req);
        Optional<ClassLoader> classLoader = CompilerClassloaderUtils.createClassloaderFromCpFiles(tmpRoot.toString() + "/dummy/");
        Assert.assertTrue(classLoader.isPresent());
    }

    @Test
    public void readFileAsURI() {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);
        compiler.compile(req);
        List<String> classpathFiles = new ArrayList<>();
        classpathFiles.add(tmpRoot + "/dummy/module.cpath");
        List<URI> uris = CompilerClassloaderUtils.processScannedFilesAsURIs(classpathFiles);
        Assert.assertTrue(uris.size() == 4);
    }
}
