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

import org.jboss.resteasy.core.AsynchronousDispatcher;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.SynchronousExecutionContext;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Assert;
import org.junit.BeforeClass;

import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.HttpCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class MavenRestHandlerTest extends BaseCompilerTest {

    public MavenRestHandlerTest(){
        super("target/test-classes/kjar-2-single-resources");
    }

    protected static Path mavenRepo;
    protected static Logger logger = LoggerFactory.getLogger(MavenRestHandlerTest.class);

    @BeforeClass
    public static void setup() throws Exception{
        mavenRepo = Paths.get(System.getProperty("user.home"), "/.m2/repository");

        if (!Files.exists(mavenRepo)) {
            logger.info("Creating a m2_repo into " + mavenRepo);
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Test
    public void get() throws Exception{
        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
        POJOResourceFactory noDefaults = new POJOResourceFactory(MavenRestHandler.class);
        dispatcher.getRegistry().addResourceFactory(noDefaults);
        MockHttpRequest request = MockHttpRequest.get("maven/3.3.9/");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Assert.assertTrue(response.getStatus() == 200);
        Assert.assertTrue(response.getContentAsString().equals("Apache Maven 3.3.9"));
    }

    @Test
    public void post() throws Exception{
        Dispatcher dispatcher = new AsynchronousDispatcher(new ResteasyProviderFactory());
        ResteasyProviderFactory.setInstance(dispatcher.getProviderFactory());
        RegisterBuiltin.register(dispatcher.getProviderFactory());

        POJOResourceFactory noDefaults = new POJOResourceFactory(MavenRestHandler.class);
        dispatcher.getRegistry().addResourceFactory(noDefaults);

        MockHttpRequest request = MockHttpRequest.create("POST", "maven/3.3.9/");
        request.header("project",tmpRoot.toAbsolutePath().toString()+"/dummy").header("mavenrepo", mavenRepo.toAbsolutePath().toString());
        MockHttpResponse response = new MockHttpResponse();

        SynchronousExecutionContext synchronousExecutionContext = new SynchronousExecutionContext((SynchronousDispatcher)dispatcher, request, response );
        request.setAsynchronousContext(synchronousExecutionContext);

        dispatcher.invoke(request, response);
        Assert.assertTrue(response.getStatus() == 200);
        byte[] serializedCompilationResponse = response.getOutput();
        HttpCompilationResponse res = RestUtils.readDefaultCompilationResponseFromBytes(serializedCompilationResponse);
        Assert.assertNotNull(res);
        Assert.assertTrue(res.getDependencies().size() == 4);
        Assert.assertTrue(res.getTargetContent().size() == 3);
    }

}