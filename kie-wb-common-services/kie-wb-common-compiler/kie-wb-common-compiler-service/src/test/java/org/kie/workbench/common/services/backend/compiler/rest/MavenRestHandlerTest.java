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
package org.kie.workbench.common.services.backend.compiler.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.junit.Assert;
import org.junit.BeforeClass;

import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.HttpCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultHttpCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.rest.server.MavenRestHandler;
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
    public void getTest() throws Exception{
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
    public void postTest() throws Exception{
        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
        POJOResourceFactory noDefaults = new POJOResourceFactory(MavenRestHandler.class);
        dispatcher.getRegistry().addResourceFactory(noDefaults);
        MockHttpRequest request = MockHttpRequest.create("POST", "maven/3.3.9/");
        request.header("project",tmpRoot.toAbsolutePath().toString()+"/dummy").header("mavenrepo", mavenRepo.toAbsolutePath().toString());
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Assert.assertTrue(response.getStatus() == 200);
        byte[] serializedCompileationResponse = response.getOutput();
        HttpCompilationResponse res =readDefaultCompiletionResponseFromBytes(serializedCompileationResponse);
        Assert.assertNotNull(res);
        Assert.assertTrue(res.getDependencies().size() == 4);
        Assert.assertTrue(res.getTargetContent().size() == 3);
    }


    public HttpCompilationResponse readDefaultCompiletionResponseFromBytes(byte[] bytes) {

        ObjectInput in = null;
        ByteArrayOutputStream bos = null;

        try {
            in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object newObj = in.readObject();
            return (HttpCompilationResponse) newObj;
        } catch (NotSerializableException nse) {
            nse.printStackTrace();
            StringBuilder sb = new StringBuilder("NotSerializableException:").append(nse.getMessage());
            logger.error(sb.toString());
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("IOException:").append(ioe.getMessage());
            logger.error(sb.toString());
        } catch (ClassNotFoundException cnfe) {
            StringBuilder sb = new StringBuilder("ClassNotFoundException:").append(cnfe.getMessage());
            logger.error(sb.toString());
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Exception:").append(e.getMessage());
            logger.error(sb.toString());
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        return new DefaultHttpCompilationResponse(Boolean.FALSE);
    }

}
