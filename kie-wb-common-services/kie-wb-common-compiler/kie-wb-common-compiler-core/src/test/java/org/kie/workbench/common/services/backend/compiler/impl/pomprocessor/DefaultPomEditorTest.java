package org.kie.workbench.common.services.backend.compiler.impl.pomprocessor;

import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.ConfigurationContextProvider;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.uberfire.java.nio.file.Paths;

public class DefaultPomEditorTest extends BaseCompilerTest {

    public DefaultPomEditorTest() {
        super("target/test-classes/kjar-2-single-resources");
    }

    @Test
    public void readSingleTest() {
        ConfigurationContextProvider provider = new ConfigurationContextProvider();
        DefaultPomEditor editor = new DefaultPomEditor(new HashSet<PomPlaceHolder>(), provider);
        Assert.assertTrue(editor.getHistory().size() == 0);
        PomPlaceHolder placeholder = editor.readSingle(Paths.get(tmpRoot.toAbsolutePath() + "/dummy/pom.xml"));
        Assert.assertTrue(placeholder.isValid());
        Assert.assertEquals(placeholder.getVersion(), "1.0.0.Final");
        Assert.assertEquals(placeholder.getPackaging(), "kjar");
        Assert.assertEquals(placeholder.getGroupID(), "org.kie");
        Assert.assertEquals(placeholder.getArtifactID(), "kie-maven-plugin-test-kjar-2");
    }

    @Test
    public void writeTest() {
        ConfigurationContextProvider provider = new ConfigurationContextProvider();
        DefaultPomEditor editor = new DefaultPomEditor(new HashSet<PomPlaceHolder>(), provider);
        Assert.assertTrue(editor.getHistory().size() == 0);

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        Assert.assertTrue(editor.write(Paths.get(tmpRoot.toAbsolutePath() + "/dummy/pom.xml"), req));
    }
}
