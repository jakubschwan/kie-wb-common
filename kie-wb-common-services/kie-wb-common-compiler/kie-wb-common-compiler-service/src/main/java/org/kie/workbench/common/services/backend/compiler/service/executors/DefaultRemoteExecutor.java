package org.kie.workbench.common.services.backend.compiler.service.executors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.guvnor.common.services.backend.cache.LRUCache;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.KieAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.decorators.OutputLogAfterDecorator;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Paths;

public class DefaultRemoteExecutor implements RemoteExecutor {

    private ExecutorService executor ;
    private LRUCache<String, CompileInfo> compilerCacheForRemoteInvocation;
    private Logger logger = LoggerFactory.getLogger(DefaultRemoteExecutor.class);
    private KieCompilationResponse defaultRes = new DefaultKieCompilationResponse(Boolean.FALSE);


    public DefaultRemoteExecutor(ExecutorService executorService){
        executor = executorService;
        compilerCacheForRemoteInvocation = new LRUCache<String, CompileInfo>(){};
    }

    private AFCompiler getCompiler(String projectPath, String mavenRepo){
        AFCompiler compiler = compilerCacheForRemoteInvocation.getEntry(projectPath).getCompiler();
        if(compiler == null){
            CompileInfo info = setupCompileInfo(projectPath, mavenRepo);
            compilerCacheForRemoteInvocation.setEntry(projectPath, info);
            compiler = info.getCompiler();
        }
        return compiler;
    }


    private CompileInfo setupCompileInfo(String workingDir, String mavenRepo) {
        AFCompiler compiler = new KieAfterDecorator(new OutputLogAfterDecorator(new BaseMavenCompiler()));
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(workingDir));
        return new CompileInfo(compiler, info,  mavenRepo);
    }



    private CompletableFuture<KieCompilationResponse> internalBuild(String projectPath, String mavenRepo,
                                                 boolean skipProjectDepCreation, String goal) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(projectPath));
        AFCompiler compiler = getCompiler(projectPath, mavenRepo);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{goal},
                                                               skipProjectDepCreation);
        return runInItsOwnThread(compiler, req);

    }

    private CompletableFuture<KieCompilationResponse> internalBuild(String projectPath, String mavenRepo,
                                                 boolean skipProjectDepCreation, String[] args) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(projectPath));
        AFCompiler compiler = getCompiler(projectPath, mavenRepo);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               skipProjectDepCreation);

        return runInItsOwnThread(compiler, req);
    }

    private CompletableFuture<KieCompilationResponse> runInItsOwnThread(AFCompiler compiler, CompilationRequest req) {
        return CompletableFuture.supplyAsync(()->((KieCompilationResponse)compiler.compile(req)), executor);
    }

    /************************************ Suitable for the REST Builds ************************************************/

    @Override
    public CompletableFuture<KieCompilationResponse> build(String projectPath, String mavenRepo) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE, MavenCLIArgs.COMPILE);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return internalBuild(projectPath, mavenRepo, skipPrjDependenciesCreationList, MavenCLIArgs.COMPILE);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath, String mavenRepo) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE, MavenCLIArgs.INSTALL);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath, String mavenRepo,
                                                  Boolean skipPrjDependenciesCreationList) {
        return internalBuild(projectPath, mavenRepo, skipPrjDependenciesCreationList, MavenCLIArgs.INSTALL);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args) {
        return internalBuild(projectPath, mavenRepo, Boolean.FALSE, args);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args,
                                                   Boolean skipPrjDependenciesCreationList) {
        return internalBuild(projectPath, mavenRepo, skipPrjDependenciesCreationList, args);
    }
}