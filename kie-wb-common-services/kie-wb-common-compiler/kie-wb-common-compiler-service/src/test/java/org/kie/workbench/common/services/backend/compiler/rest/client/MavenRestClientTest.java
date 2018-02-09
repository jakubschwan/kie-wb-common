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
package org.kie.workbench.common.services.backend.compiler.rest.client;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.services.backend.compiler.HttpCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.rest.RestUtils;
import org.kie.workbench.common.services.backend.compiler.rest.server.MavenRestHandler;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

@RunWith(Arquillian.class)
public class MavenRestClientTest {

    private  Path tmpRoot;
    private  Path mavenRepo;

    @ArquillianResource
    private URL deploymentUrl;

    @Before
    public  void setup() throws Exception{
        tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        FileUtils.copyDirectory(new File("kie-wb-common-services/kie-wb-common-compiler/kie-wb-common-compiler-service/target/test-classes/kjar-2-single-resources"), tmp.toFile());
        mavenRepo =   Paths.get(System.getProperty("user.home"), "/.m2/repository");
    }

    @After
    public void tearDown()  {
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
                         "org.eclipse.jgit:org.eclipse.jgit:?",
                         "org.jboss.resteasy:resteasy-jaxrs:?",
                         "org.jboss.resteasy:resteasy-multipart-provider:?").withTransitivity()
                .asFile();

        for (final File file : files) {
            war.addAsLibrary(file);
        }
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    public void get() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(deploymentUrl.toString() +"rest/maven/3.3.9/");
        Invocation invocation = target.request().buildGet();
        Response response = invocation.invoke();
        Assert.assertEquals(response.getStatusInfo().getStatusCode(), 200);
        Assert.assertEquals("Apache Maven 3.3.9", response.readEntity(String.class));
    }


    @Test
    public void post() throws Exception{
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(deploymentUrl.toString() +"rest/maven/3.3.9/");
        MultivaluedMap headersMap = new MultivaluedHashMap();
        headersMap.add("project", tmpRoot.toAbsolutePath().toString()+"/dummy");
        headersMap.add("mavenrepo", mavenRepo.toAbsolutePath().toString());
        Future<Response>  responseFuture = target.request().headers(headersMap).async().post(Entity.entity(String.class, MediaType.TEXT_PLAIN));
        Response response = responseFuture.get();
        Assert.assertEquals(response.getStatusInfo().getStatusCode(), 200);
        InputStream is = response.readEntity(InputStream.class);
        byte[] serializedCompilationResponse = IOUtils.toByteArray(is);;
        HttpCompilationResponse res = RestUtils.readDefaultCompilationResponseFromBytes(serializedCompilationResponse);
        Assert.assertNotNull(res);
        Assert.assertTrue(res.getDependencies().size() == 4);
        Assert.assertTrue(res.getTargetContent().size() == 3);
    }
}