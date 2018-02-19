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
package org.kie.workbench.common.services.backend.maven.plugins.dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.repository.RepositoryManager;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This goal will output a classpath string of dependencies from the local repository to a memory map.
 * Original version:
 * https://github.com/apache/maven-plugins/blob/trunk/maven-dependency-plugin/src/main/java/org/apache/maven/plugins/dependency/fromDependencies/BuildClasspathMojo.java
 * IMPORTANT: Preserve the structure for an easy update when the maven version will be updated
 */
// CHECKSTYLE_OFF: LineLength
@Mojo(name = "build-classpath", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
// CHECKSTYLE_ON: LineLength
public class BuildInMemoryClasspathMojo
        extends AbstractDependencyFilterMojo
        implements Comparator<Artifact> {

    /**
     * Key used to share the string classpath in the kieMap
     */
    private final String STRING_CLASSPATH_KEY = "stringClasspathKey";
    /**
     * Strip artifact version during copy (only works if prefix is set)
     */
    @Parameter(property = "mdep.stripVersion", defaultValue = "false")
    private boolean stripVersion = false;
    /**
     * Strip artifact classifier during copy (only works if prefix is set)
     */
    @Parameter(property = "mdep.stripClassifier", defaultValue = "false")
    private boolean stripClassifier = false;
    /**
     * The prefix to prepend on each dependent artifact. If undefined, the paths refer to the actual files store in the
     * local repository (the stripVersion parameter does nothing then).
     */
    @Parameter(property = "mdep.prefix")
    private String prefix;
    /**
     * A property to set to the content of the classpath string.
     */
    @Parameter(property = "mdep.outputProperty")
    private String outputProperty;
    /**
     * Override the char used between the paths. This field is initialized to contain the first character of the value
     * of the system property file.separator. On UNIX systems the value of this field is '/'; on Microsoft Windows
     * systems it is '\'. The default is File.separator
     * @since 2.0
     */
    @Parameter(property = "mdep.fileSeparator", defaultValue = "")
    private String fileSeparator;
    /**
     * Override the char used between path folders. The system-dependent path-separator character. This field is
     * initialized to contain the first character of the value of the system property path.separator. This character is
     * used to separate filenames in a sequence of files given as a path list. On UNIX systems, this character is ':';
     * on Microsoft Windows systems it is ';'.
     * @since 2.0
     */
    @Parameter(property = "mdep.pathSeparator", defaultValue = "")
    private String pathSeparator;
    /**
     * Replace the absolute path to the local repo with this property. This field is ignored it prefix is declared. The
     * value will be forced to "${M2_REPO}" if no value is provided AND the attach flag is true.
     * @since 2.0
     */
    @Parameter(property = "mdep.localRepoProperty", defaultValue = "")
    private String localRepoProperty;
    /**
     * Write out the classpath in a format compatible with filtering (classpath=xxxxx)
     * @since 2.0
     */
    @Parameter(property = "mdep.outputFilterFile", defaultValue = "false")
    private boolean outputFilterFile;
    /**
     * Either append the artifact's baseVersion or uniqueVersion to the filename. Will only be used if
     * {@link #isStripVersion()} is {@code false}.
     * @since 2.6
     */
    @Parameter(property = "mdep.useBaseVersion", defaultValue = "true")
    private boolean useBaseVersion = true;
    /**
     * Maven ProjectHelper
     */
    @Component
    private MavenProjectHelper projectHelper;
    @Component
    private RepositoryManager repositoryManager;
    /**
     * This container is the same accessed in the KieMavenCli in the kie-wb-common
     */
    @Inject
    private PlexusContainer container;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates to create a classpath.
     * @throws MojoExecutionException with a message if an error occurs.
     * @see #getResolvedDependencies(boolean)
     */
    @Override
    protected void doExecute()
            throws MojoExecutionException {
        // initialize the separators.
        boolean isFileSepSet = StringUtils.isNotEmpty(fileSeparator);
        boolean isPathSepSet = StringUtils.isNotEmpty(pathSeparator);

        // don't allow them to have absolute paths when they attach.
        if (StringUtils.isEmpty(localRepoProperty)) {
            localRepoProperty = "${M2_REPO}";
        }

        Set<Artifact> artifacts = getResolvedDependencies(true);

        if (artifacts == null || artifacts.isEmpty()) {
            getLog().info("No dependencies found.");
        }

        List<Artifact> artList = new ArrayList<Artifact>(artifacts);

        StringBuilder sb = new StringBuilder();
        Iterator<Artifact> i = artList.iterator();

        if (i.hasNext()) {
            appendArtifactPath(i.next(), sb);

            while (i.hasNext()) {
                sb.append(isPathSepSet ? this.pathSeparator : File.pathSeparator);
                appendArtifactPath(i.next(), sb);
            }
        }

        String cpString = sb.toString();

        // if file separator is set, I need to replace the default one from all
        // the file paths that were pulled from the artifacts
        if (isFileSepSet) {
            // Escape file separators to be used as literal strings
            final String pattern = Pattern.quote(File.separator);
            final String replacement = Matcher.quoteReplacement(fileSeparator);
            cpString = cpString.replaceAll(pattern, replacement);
        }

        // make the string valid for filtering
        if (outputFilterFile) {
            cpString = "classpath=" + cpString;
        }

        if (outputProperty != null) {
            getProject().getProperties().setProperty(outputProperty, cpString);
            if (getLog().isDebugEnabled()) {
                getLog().debug(outputProperty + " = " + cpString);
            }
        }

        storeClasspathFile(cpString);
    }

    /**
     * Appends the artifact path into the specified StringBuilder.
     * @param art {@link Artifact}
     * @param sb {@link StringBuilder}
     */
    protected void appendArtifactPath(Artifact art, StringBuilder sb) {
        if (prefix == null) {
            String file = art.getFile().getPath();
            // substitute the property for the local repo path to make the classpath file portable.
            if (StringUtils.isNotEmpty(localRepoProperty)) {
                File localBasedir = repositoryManager.getLocalRepositoryBasedir(session.getProjectBuildingRequest());

                file = StringUtils.replace(file, localBasedir.getAbsolutePath(), localRepoProperty);
            }
            sb.append(file);
        } else {
            // TODO: add param for prepending groupId and version.
            sb.append(prefix);
            sb.append(File.separator);
            sb.append(DependencyUtil.getFormattedFileName(art, this.stripVersion, this.prependGroupId,
                                                          this.useBaseVersion, this.stripClassifier));
        }
    }

    /**
     * It stores the specified string into the kieMap.
     * @param cpString the string to be written into the map.
     */
    private void storeClasspathFile(String cpString) {
        if (container != null) {
            Optional<Map<String, Object>> optionalKieMap = getKieMap();
            if (optionalKieMap.isPresent()) {
                String compilationID = getCompilationID(optionalKieMap);
                shareStringClasspathWithMap(compilationID, cpString);
            } else {
                getLog().error("Kie Map not present");
            }
        }
    }

    private Optional<Map<String, Object>> getKieMap() {
        try {
            /**
             * Retrieve the map passed into the Plexus container by the MavenEmbedder from the MavenIncrementalCompiler in the kie-wb-common
             */
            Map<String, Object> kieMap = (Map<String,Object>) container.lookup(Map.class,
                                                                "java.util.HashMap",
                                                                "kieMap");
            return Optional.of(kieMap);
        } catch (ComponentLookupException cle) {
            getLog().info("kieMap not present with compilationID and container present");
            return Optional.empty();
        }
    }

    private String getCompilationID(Optional<Map<String, Object>> optionalKieMap) {
        Object compilationIDObj = optionalKieMap.get().get("compilation.ID");
        if (compilationIDObj != null) {
            return compilationIDObj.toString();
        } else {
            getLog().error("compilation.ID key not present in the shared map using thread name:"
                                   + Thread.currentThread().getName());
            return Thread.currentThread().getName();
        }
    }

    private void shareStringClasspathWithMap(String compilationID, String classpath) {
        Optional<Map<String, Object>> optionalKieMap = getKieMap();
        if (optionalKieMap.isPresent()) {
            /*Standard for the kieMap keys -> compilationID + dot + class name or name of the variable if is a String */
            StringBuilder stringClasspathKey = new StringBuilder(compilationID).append(".").append(STRING_CLASSPATH_KEY);
            optionalKieMap.get().put(stringClasspathKey.toString(), classpath);
            getLog().info("String Classpath available in the map shared with the Maven Embedder with key:" + stringClasspathKey.toString());
        }
    }

    /**
     * Compares artifacts lexicographically, using pattern [group_id][artifact_id][version].
     * @param art1 first object
     * @param art2 second object
     * @return the value <code>0</code> if the argument string is equal to this string; a value less than <code>0</code>
     * if this string is lexicographically less than the string argument; and a value greater than
     * <code>0</code> if this string is lexicographically greater than the string argument.
     */
    @Override
    public int compare(Artifact art1, Artifact art2) {
        if (art1 == art2) {
            return 0;
        } else if (art1 == null) {
            return -1;
        } else if (art2 == null) {
            return +1;
        }

        String s1 = art1.getGroupId() + art1.getArtifactId() + art1.getVersion();
        String s2 = art2.getGroupId() + art2.getArtifactId() + art2.getVersion();

        return s1.compareTo(s2);
    }

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter() {
        return null;
    }

    /**
     * @param theOutputProperty the outputProperty to set
     */
    public void setOutputProperty(String theOutputProperty) {
        this.outputProperty = theOutputProperty;
    }

    /**
     * @param theFileSeparator the fileSeparator to set
     */
    public void setFileSeparator(String theFileSeparator) {
        this.fileSeparator = theFileSeparator;
    }

    /**
     * @param thePathSeparator the pathSeparator to set
     */
    public void setPathSeparator(String thePathSeparator) {
        this.pathSeparator = thePathSeparator;
    }

    /**
     * @param thePrefix the prefix to set
     */
    public void setPrefix(String thePrefix) {
        this.prefix = thePrefix;
    }

    /**
     * @return the stripVersion
     */
    public boolean isStripVersion() {
        return this.stripVersion;
    }

    /**
     * @param theStripVersion the stripVersion to set
     */
    public void setStripVersion(boolean theStripVersion) {
        this.stripVersion = theStripVersion;
    }

    public void setLocalRepoProperty(String localRepoProperty) {
        this.localRepoProperty = localRepoProperty;
    }
}
