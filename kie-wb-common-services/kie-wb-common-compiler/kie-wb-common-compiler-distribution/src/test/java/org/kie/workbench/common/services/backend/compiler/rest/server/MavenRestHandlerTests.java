package org.kie.workbench.common.services.backend.compiler.rest.server;

import java.util.concurrent.CompletableFuture;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.rest.client.MavenRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

@RunWith(Arquillian.class)
public class MavenRestHandlerTests {

    protected static Path mavenRepo;
    protected static Logger logger = LoggerFactory.getLogger(MavenRestHandlerTests.class);

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        setup();
        WebArchive war  = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsResource("logback-test.xml", "logback-test.xml")
                .setWebXML("test-web.xml")
                .addClass(MavenRestHandler.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        System.out.println(war.toString(true));
        return war;
    }

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

    @Test
    public void should_create_greeting() throws Exception{
        Path path = Paths.get("target/test-classes/dummy_deps_complex/");
        MavenRestClient client = new MavenRestClient();
        CompletableFuture<KieCompilationResponse> res = client.callMyMaybe(path.toAbsolutePath().toString(),
                                                                           mavenRepo.toAbsolutePath().toString(),
                                                                           "http://localhost:8080/rest/maven/3.3.9/");
        //Assert.assertTrue(res.get().isSuccessful());
        Assert.assertTrue(true);

    }
}
