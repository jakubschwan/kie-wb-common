package org.kie.workbench.common.services.backend.service.executors;

import java.util.concurrent.ExecutorService;

import org.guvnor.common.services.backend.cache.LRUCache;
import org.kie.workbench.common.services.backend.builder.af.KieAFBuilder;
import org.kie.workbench.common.services.backend.builder.af.impl.DefaultKieAFBuilder;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.uberfire.java.nio.file.Paths;

public class DefaultRemoteExecutor implements RemoteExecutor {

    private ExecutorService executor ;
    private LRUCache<String, KieAFBuilder> cacheForRemoteInvocation;


    public DefaultRemoteExecutor(ExecutorService executorService){
        executor = executorService;
        cacheForRemoteInvocation = new LRUCache<String, KieAFBuilder>(){};
    }

    private KieAFBuilder getBuilder(String projectPath, String mavenRepo){
        KieAFBuilder builder = cacheForRemoteInvocation.getEntry(projectPath);
        if(builder == null){
            builder = new DefaultKieAFBuilder(Paths.get(projectPath), mavenRepo);
            cacheForRemoteInvocation.setEntry(projectPath, builder);
        }
        return builder;
    }


    /************************************ Suitable for the REST Builds ************************************************/

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo) {
        return getBuilder(projectPath, mavenRepo).build(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        return getBuilder(projectPath, mavenRepo).build(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo) {
        return getBuilder(projectPath, mavenRepo).buildAndInstall(projectPath, mavenRepo);
    }

    @Override
    public KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo,
                                                  Boolean skipPrjDependenciesCreationList) {
        return getBuilder(projectPath, mavenRepo).buildAndInstall(projectPath, mavenRepo,skipPrjDependenciesCreationList);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args) {
        return getBuilder(projectPath, mavenRepo).buildSpecialized(projectPath, mavenRepo, args);
    }

    @Override
    public KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args,
                                                   Boolean skipPrjDependenciesCreationList) {
        return getBuilder(projectPath, mavenRepo).buildSpecialized(projectPath, mavenRepo, args, skipPrjDependenciesCreationList);
    }
}
