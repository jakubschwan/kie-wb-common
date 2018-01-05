/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.backend.compiler.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.codehaus.plexus.classworlds.ClassWorld;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenConfig;
import org.kie.workbench.common.services.backend.compiler.impl.external339.ReusableAFMavenCli;
import org.kie.workbench.common.services.backend.compiler.impl.incrementalenabler.DefaultIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.compiler.impl.incrementalenabler.IncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.compiler.impl.pomprocessor.ProcessedPoms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;

/**
 * Run maven with https://maven.apache.org/ref/3.3.9/maven-embedder/xref/index.html
 * to use Takari plugins like a black box
 * <p>
 * MavenCompiler compiler = new DefaultMavenCompiler();
 * or
 * MavenCompiler compiler = MavenCompilerFactory.getCompiler(Decorator.LOG_OUTPUT_AFTER);
 * <p>
 * WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmp);
 * CompilationRequest req = new DefaultCompilationRequest(mavenRepo, info,new String[]{MavenArgs.COMPILE},new HashMap<>(), Boolean.TRUE );
 * CompilationResponse res = compiler.compileSync(req);
 */
public class BaseMavenCompiler<T extends CompilationResponse> implements AFCompiler<T> {

    private static final Logger logger = LoggerFactory.getLogger(BaseMavenCompiler.class);
    private int writeBlockSize = 1024;
    //private AFMavenCli cli;
    private ReusableAFMavenCli cli;

    private IncrementalCompilerEnabler enabler;

    public BaseMavenCompiler() {
        cli = new ReusableAFMavenCli();
        //cli = new AFMavenCli();
        enabler = new DefaultIncrementalCompilerEnabler();
    }

    /**
     * Check if the folder exists and if it's writable and readable
     * @param mavenRepo
     * @return
     */
    public static Boolean isValidMavenRepo(final Path mavenRepo) {
        if (mavenRepo.getParent() == null) {
            return Boolean.FALSE;// used because Path("") is considered for Files.exists...
        }
        return Files.exists(mavenRepo) && Files.isDirectory(mavenRepo) && Files.isWritable(mavenRepo) && Files.isReadable(mavenRepo);
    }

    public Boolean cleanInternalCache() {
        return enabler.cleanHistory() && cli.cleanInternals();
    }

    public void invalidatePomHistory() {
        enabler.cleanHistory();
        cli.cleanInternals();
    }

    @Override
    public T compile(CompilationRequest req) {
        MDC.clear();
        MDC.put(MavenConfig.COMPILATION_ID, req.getRequestUUID());
        Thread.currentThread().setName(req.getRequestUUID());
        if (logger.isDebugEnabled()) {
            logger.debug("KieCompilationRequest:{}", req);
        }

        if (!req.getInfo().getEnhancedMainPomFile().isPresent()) {
            ProcessedPoms processedPoms = enabler.process(req);
            if (!processedPoms.getResult()) {
                List<String> msgs = new ArrayList<>(1);
                msgs.add("[ERROR] Processing poms failed");
                return (T) new DefaultKieCompilationResponse(Boolean.FALSE,
                                                             msgs,
                                                             req.getInfo().getPrjPath());
            }
        }
        req.getKieCliRequest().getRequest().setLocalRepositoryPath(req.getMavenRepo());
        /**
         The classworld is now Created in the DefaultMaven compiler for this reasons:
         problem: https://stackoverflow.com/questions/22410706/error-when-execute-mavencli-in-the-loop-maven-embedder
         problem:https://stackoverflow.com/questions/40587683/invocation-of-mavencli-fails-within-a-maven-plugin
         solution:https://dev.eclipse.org/mhonarc/lists/sisu-users/msg00063.html
         */

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassWorld kieClassWorld = new ClassWorld("plexus.core",
                                                  getClass().getClassLoader());
        int exitCode = cli.doMain(req.getKieCliRequest(),
                                  kieClassWorld);
        Thread.currentThread().setContextClassLoader(original);
        if (exitCode == 0) {
            return (T) new DefaultKieCompilationResponse(Boolean.TRUE);
        } else {
            return (T) new DefaultKieCompilationResponse(Boolean.FALSE);
        }
    }

    @Override
    public T compile(CompilationRequest req, Map<java.nio.file.Path, InputStream> override) {
        for (Map.Entry<java.nio.file.Path, InputStream> entry : override.entrySet()) {
            java.nio.file.Path path = entry.getKey();
            InputStream input = entry.getValue();
            try {
                java.nio.file.Files.write(path, readAllBytes(input));
            }catch (IOException e){
                logger.error("Path not writed:"+entry.getKey()+ "\n");
                logger.error(e.getMessage());
                logger.error("\n");
            }
        }
        return compile(req);
    }

    public byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        out.close();
        return out.toByteArray();
    }

    public void copy(InputStream in, OutputStream out) throws IOException
    {
        byte[] bytes = new byte[writeBlockSize];
        int len;
        while ((len = in.read(bytes)) != -1) out.write(bytes, 0, len);
    }
}