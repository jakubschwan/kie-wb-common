package org.kie.workbench.common.services.backend.compiler.rest.client;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.rest.server.MavenRestHandler;
import org.kie.workbench.common.services.backend.compiler.rest.server.MavenRestHandlerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

@RunWith(Arquillian.class)
public class MavenRestClientTest extends BaseCompilerTest {

    public MavenRestClientTest(){
        super("target/test-classes/kjar-2-single-resources");
    }

    protected static Path mavenRepo;
    protected static Logger logger = LoggerFactory.getLogger(MavenRestHandlerTest.class);

    @BeforeClass
    public static void setup() throws Exception{
        mavenRepo = Paths.get(System.getProperty("user.home"),
                              "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "compiler.war");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addClasses(MavenRestHandler.class);
        war.addPackages(true, "org.kie.workbench.common.services.backend.compiler");
        final File[] files = Maven.configureResolver().
                fromFile("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/src/test/settings.xml").
                loadPomFromFile("./pom.xml")
                .resolve("org.kie.workbench.services:kie-wb-common-compiler-core:?",
                         "org.jboss.errai:errai-bus:?", "org.uberfire:uberfire-nio2-api:?").withTransitivity()
                .asFile();
        for (final File file : files) {
            war.addAsLibrary(file);
        }
        System.out.println(war.toString(true));
        return war;
    }

    @Test @Ignore
    public void get() {
        Assert.assertTrue(true);
        /*Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://127.0.0.1:8080/maven/3.3.9/");
        Invocation invocation = target.request().buildGet();
        Response response = invocation.invoke();
        Assert.assertEquals("Apache Maven 3.3.9", response.readEntity(String.class));*/
    }

}
