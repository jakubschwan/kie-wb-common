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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.service.AFCompilerService;
import org.kie.workbench.common.services.backend.compiler.service.DefaultKieCompilerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Paths;

@Path("/maven/3.3.9/")
@RequestScoped
public class MavenRestHandler extends Application{

    private static Logger logger = LoggerFactory.getLogger(MavenRestHandler.class);

    private static String  mvn = "Apache Maven 3.3.9";

    private AFCompilerService compilerService;

    public MavenRestHandler(){
        compilerService = new DefaultKieCompilerService();
    }

    @GET
    @Produces("text/plain")
    public String get() {
        return mvn;
    }

    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] post(@HeaderParam("project") String projectRepo, @HeaderParam("mavenrepo") String mavenRepo) throws Exception {
        CompletableFuture<KieCompilationResponse> response = compilerService.build(Paths.get(projectRepo), mavenRepo);
        return serialize(response.get());//@TODO remove this .get()
    }

    @PUT
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void postAsync(@Suspended AsyncResponse ar, @HeaderParam("project") String projectRepo, @HeaderParam("mavenrepo") String mavenRepo) throws Exception {
        CompletableFuture<KieCompilationResponse> response = compilerService.build(Paths.get(projectRepo), mavenRepo);
        ar.resume(Response.ok(serialize(response)).build());
    }


    public static byte[] serialize(Object obj) throws IOException {
        byte[] returnArray = null;
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            returnArray = b.toByteArray();
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
        return  returnArray;
    }

}
