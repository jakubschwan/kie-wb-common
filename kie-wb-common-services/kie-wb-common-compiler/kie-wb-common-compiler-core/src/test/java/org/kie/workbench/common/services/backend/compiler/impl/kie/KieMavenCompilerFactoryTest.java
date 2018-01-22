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
package org.kie.workbench.common.services.backend.compiler.impl.kie;

import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.JGITCompilerBeforeDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.KieAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.OutputLogAfterDecorator;

public class KieMavenCompilerFactoryTest {

    @Test
    public void noneTest() {
        AFCompiler none = KieMavenCompilerFactory.getCompiler(KieDecorator.NONE);
        Assert.assertTrue(none instanceof BaseMavenCompiler);
    }

    @Test
    public void logOutputAfterDecoratorTest() {
        AFCompiler logAfter = KieMavenCompilerFactory.getCompiler(KieDecorator.LOG_OUTPUT_AFTER);
        Assert.assertTrue(logAfter instanceof OutputLogAfterDecorator);
    }

    @Test
    public void kieAfterDecoratorTest() {
        AFCompiler kieAfter = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AFTER);
        Assert.assertTrue(kieAfter instanceof KieAfterDecorator);
    }

    @Test
    public void jGitBeforeDecoratorTest() {
        AFCompiler jgitBefore = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE);
        Assert.assertTrue(jgitBefore instanceof JGITCompilerBeforeDecorator);
    }

    @Test
    public void kieAndLogAfterDecoratorTest() {
        AFCompiler kieAfterDecorator = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AND_LOG_AFTER);
        Assert.assertTrue(kieAfterDecorator instanceof KieAfterDecorator);
        AFCompiler outputLofAfterDecorator = ((KieAfterDecorator) kieAfterDecorator).getCompiler();
        Assert.assertTrue(outputLofAfterDecorator instanceof OutputLogAfterDecorator);
        AFCompiler baseMavenCompiler = ((OutputLogAfterDecorator) outputLofAfterDecorator).getCompiler();
        Assert.assertTrue(baseMavenCompiler instanceof BaseMavenCompiler);
    }

    @Test
    public void jgitBeforeAndLogAfterDecoratorTest() {
        AFCompiler jgitBeforeAndLogAfter = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_LOG_AFTER);
        Assert.assertTrue(jgitBeforeAndLogAfter instanceof JGITCompilerBeforeDecorator);
        AFCompiler outputLofAfterDecorator = ((JGITCompilerBeforeDecorator) jgitBeforeAndLogAfter).getCompiler();
        Assert.assertTrue(outputLofAfterDecorator instanceof OutputLogAfterDecorator);
        AFCompiler baseMavenCompiler = ((OutputLogAfterDecorator) outputLofAfterDecorator).getCompiler();
        Assert.assertTrue(baseMavenCompiler instanceof BaseMavenCompiler);
    }

    @Test
    public void jgitBeforeAndKieAfterDecoratorTest() {
        AFCompiler jgitBeforeAndLogAfter = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_KIE_AFTER);
        Assert.assertTrue(jgitBeforeAndLogAfter instanceof JGITCompilerBeforeDecorator);
        AFCompiler kieAfterDecorator = ((JGITCompilerBeforeDecorator) jgitBeforeAndLogAfter).getCompiler();
        Assert.assertTrue(kieAfterDecorator instanceof KieAfterDecorator);
        AFCompiler baseMavenCompiler = ((KieAfterDecorator) kieAfterDecorator).getCompiler();
        Assert.assertTrue(baseMavenCompiler instanceof BaseMavenCompiler);
    }

    @Test
    public void jgitBeforeAndKieAndLogAfterDecoratorTest() {
        AFCompiler jgitBeforeAndLogAfter = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_KIE_AND_LOG_AFTER);
        Assert.assertTrue(jgitBeforeAndLogAfter instanceof JGITCompilerBeforeDecorator);
        AFCompiler kieAfterDecorator = ((JGITCompilerBeforeDecorator) jgitBeforeAndLogAfter).getCompiler();
        Assert.assertTrue(kieAfterDecorator instanceof KieAfterDecorator);
        AFCompiler outputLofAfterDecorator = ((KieAfterDecorator) kieAfterDecorator).getCompiler();
        Assert.assertTrue(outputLofAfterDecorator instanceof OutputLogAfterDecorator);
        AFCompiler baseMavenCompiler = ((OutputLogAfterDecorator) outputLofAfterDecorator).getCompiler();
        Assert.assertTrue(baseMavenCompiler instanceof BaseMavenCompiler);
    }
}
