package org.kie.workbench.common.services.backend.compiler;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
/**
 * Wrapper of the result of a compilation adapted for the http protocol and without NIO objects
 */
public interface HttpCompilationResponse {
    Boolean isSuccessful();

    /**
     * Provides Maven output
     */
    List<String> getMavenOutput();

    /**
     * Provides the path of the working directory
     */
    Optional<String> getWorkingDir();

    /**
     * Provides the List of project dependencies from target folders as List of String
     * @return
     */
    List<String> getDependencies();

    /**
     * Provides the list of all dependencies used by the project, included transitive
     */
    List<URI> getDependenciesAsURI();

    /**
     * Provides the list of all dependencies used by the project, included transitive
     */
    List<URL> getDependenciesAsURL();

    /**
     * Provides the List of project dependencies from target folders as List of String
     * @return
     */
    List<String> getTargetContent();

    /**
     * Provides the list of all dependencies used by the project, included transitive
     */
    List<URI> getTargetContentAsURI();

    /**
     * Provides the list of all dependencies used by the project, included transitive
     */
    List<URL> getTargetContentAsURL();
}
