package org.kie.workbench.common.services.backend.service.executors;

import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;

/***
 * This interface provides behaviour and use simple objects from HTTP world to run build requested from a remote client
 */
public interface RemoteExecutor {

    /************************************ Suitable for the REST Builds ************************************************/


    /**
     * Run a mvn compile on the projectPath with mavenRepo specified, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    KieCompilationResponse build(String projectPath, String mavenRepo);


    /**
     * Run a mvn compile on the projectPath with mavenRepo specified, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    KieCompilationResponse build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList);


    /**
     * Run a mvn install on the projectPath, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo);


    /**
     * Run a mvn install on the projectPath, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder and maven repo changes
     * between compilation Requests
     */
    KieCompilationResponse buildAndInstall(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList);


    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder, maven repo and
     * maven args changes between compilation Requests
     */
    KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo, String[] args);


    /**
     * Run a mvn {args}, maven output provided in the CompilationResponse
     * a new CompilationRequest will be created at every invocation, useful if the project folder, maven repo and
     * maven args changes between compilation Requests
     */
    KieCompilationResponse buildSpecialized(String projectPath, String mavenRepo,
                                            String[] args, Boolean skipPrjDependenciesCreationList);

}
