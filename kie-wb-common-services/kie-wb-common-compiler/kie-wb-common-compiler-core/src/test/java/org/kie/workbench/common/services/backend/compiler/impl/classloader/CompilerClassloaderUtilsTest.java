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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.utils.MavenUtils;

public class CompilerClassloaderUtilsTest extends BaseCompilerTest {

    public CompilerClassloaderUtilsTest() {
        super("target/test-classes/kjar-2-single-resources", KieDecorator.KIE_AND_CLASSPATH_AFTER_DEPS);
    }

    @Test
    public void getStringFromTargets() {
        List<String> resources = CompilerClassloaderUtils.getStringFromTargets(tmpRoot);
        assertThat(resources).hasSize(3);
    }


    @Test
    public void filterClassesByPackage() {
        List<String> targets = new ArrayList<>(3);
        targets.add("/target/classes/org/kie/test/A.class");
        targets.add("/target/classes/io/akka/test/C.class");
        targets.add("/target/classes/com/acme/test/D.class");

        List<String> orgKie = CompilerClassloaderUtils.filterClassesByPackage(targets, "org.kie");
        assertThat(orgKie).hasSize(1);

        List<String> akkaTest = CompilerClassloaderUtils.filterClassesByPackage(targets, "akka.test");
        assertThat(akkaTest).hasSize(1);

        List<String> com = CompilerClassloaderUtils.filterClassesByPackage(targets, "com");
        assertThat(com).hasSize(1);

        List<String> it = CompilerClassloaderUtils.filterClassesByPackage(targets, "it");
        assertThat(it).isEmpty();
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
        assertThat(orgKie.size() == 4).isTrue();
    }

    @Test
    public void loadDependenciesClassloaderFromProject() {
        Optional<ClassLoader> classloader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(tmpRoot.toString(), mavenRepo.toString());
        assertThat(classloader.isPresent()).isTrue();
    }

    @Test
    public void loadDependenciesClassloaderFromProjectWithPomList() {
        List<String> pomList = MavenUtils.searchPoms(tmpRoot);
        assertThat(pomList).hasSize(1);
        Optional<ClassLoader> classloader = CompilerClassloaderUtils.loadDependenciesClassloaderFromProject(pomList, mavenRepo.toString());
        assertThat(classloader.isPresent()).isTrue();
    }

    @Test
    public void getClassloaderFromProjectTargets() {
        List<String> pomList = MavenUtils.searchPoms(tmpRoot);
        Optional<ClassLoader> classLoader = CompilerClassloaderUtils.getClassloaderFromProjectTargets(pomList);
        assertThat(classLoader.isPresent()).isTrue();
    }

    @Test
    public void getClassloaderFromAllDependencies() {
        Optional<ClassLoader> classLoader = CompilerClassloaderUtils.getClassloaderFromAllDependencies(tmpRoot.toString() + "/dummy", mavenRepo.toString());
        assertThat(classLoader.isPresent()).isTrue();
    }

    @Test
    public void createClassloaderFromCpFiles() {
        assertThat(!res.getDependencies().isEmpty()).isTrue();
        assertThat(res.getDependencies().size()).isEqualTo(4);
        Optional<ClassLoader> classLoader = CompilerClassloaderUtils.createClassloaderFromStringDeps(res.getDependencies());
        assertThat(classLoader.isPresent()).isTrue();
        assertThat(classLoader.get()).isNotNull();

    }

    @Test
    public void readFileAsURI() {
        assertThat(!res.getDependencies().isEmpty()).isTrue();
        List<String> projectDeps = res.getDependencies();
        List<URI> uris = CompilerClassloaderUtils.readAllDepsAsUris(projectDeps);
        assertThat(uris).hasSize(4);
    }
}
