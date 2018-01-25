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
package org.kie.workbench.common.services.backend.compiler.rest.server;

import java.util.concurrent.CompletableFuture;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.rest.client.MavenRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class MavenRestHandlerTestLive {

    private static UndertowJaxrsServer server;
    protected static Path mavenRepo;
    protected static Logger logger = LoggerFactory.getLogger(MavenRestHandlerTests.class);

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

    @Test
    public void test() throws Exception{
        server.deploy(MavenRestHandler.class);

        MavenRestClient client = new MavenRestClient();
        Path path = Paths.get("target/test-classes/dummy_deps_complex/");
        CompletableFuture<KieCompilationResponse> res = client.callMyMaybe(path.toAbsolutePath().toString(),
                                                                           mavenRepo.toAbsolutePath().toString(),
                                                                           "http://localhost:8081/rest/maven/3.3.9/");
        Assert.assertTrue(res.get().isSuccessful());

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new UndertowJaxrsServer().start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if(server != null) {
            server.stop();
        }

    }
}
