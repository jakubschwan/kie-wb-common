package org.kie.workbench.common.services.backend.compiler.rest.client;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.rest.server.MavenRestHandler;
import org.kie.workbench.common.services.backend.compiler.rest.server.MavenRestHandlerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

@RunWith(Arquillian.class)
public class MavenRestClientTest /*extends BaseCompilerTest*/ {

    protected static Logger logger = LoggerFactory.getLogger(MavenRestHandlerTest.class);
    protected static Path tmpRoot;
    protected String alternateSettingsAbsPath;
    protected WorkspaceCompilationInfo info;
    protected AFCompiler compiler;

    @Before
    public  void setup() throws Exception{
        tmpRoot = Files.createTempDirectory("repo");
        alternateSettingsAbsPath = new File("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/target/test-classes/settings.xml").getAbsolutePath();
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        FileUtils.copyDirectory(new File("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/target/test-classes/kjar-2-single-resources"), tmp.toFile());
        info = new WorkspaceCompilationInfo(org.uberfire.java.nio.file.Paths.get(tmp.toUri()));
    }

    @AfterClass
    public static void tearDown()  {
        TestUtil.rm(new File("src/../.security/"));
    }

    @Deployment
    public static Archive getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "compiler.war");
        war.addAsResource(new File("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/target/test-classes/IncrementalCompiler.properties"));
        war.addClasses(MavenRestHandler.class);
        war.addPackages(true, "org.kie.workbench.common.services.backend.compiler");
        war.setWebXML(new File("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/target/test-classes/web.xml"));
        final File[] metaInfFilesFiles = new File("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/target/test-classes/META-INF").listFiles();
        for (final File file : metaInfFilesFiles) {
            war.addAsManifestResource(file);
        }

        final File[] files = Maven.configureResolver().
                fromFile("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/src/test/settings.xml").
                loadPomFromFile("./pom.xml")
                .resolve("org.kie.workbench.services:kie-wb-common-compiler-core:?",
                         "org.jboss.errai:errai-bus:?",
                         "org.jboss.errai:errai-jboss-as-support:?",
                         "org.jboss.errai:errai-marshalling:?",
                         "org.uberfire:uberfire-nio2-api:?",
                         "org.uberfire:uberfire-nio2-model:?",
                         "org.uberfire:uberfire-nio2-jgit:?",
                         "org.uberfire:uberfire-nio2-fs:?",
                         "org.uberfire:uberfire-servlet-security:?",
                         "org.uberfire:uberfire-testing-utils:?",
                         "org.eclipse.jgit:org.eclipse.jgit:?").withTransitivity()
                .asFile();

        for (final File file : files) {
            war.addAsLibrary(file);
        }
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    public void get() {
        System.out.println("TEST GET !!!!!!!!!!!!!!!!");
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://127.0.0.1:8080/compiler/maven/3.3.9/");
        Invocation invocation = target.request().buildGet();
        Response response = invocation.invoke();
        Assert.assertEquals("Apache Maven 3.3.9", response.readEntity(String.class));
    }



}
