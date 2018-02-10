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
package org.kie.workbench.common.services.backend.compiler.configuration;

/**
 * Common params used by Maven
 */
public class MavenConfig {

    public static final String DEPS_BUILD_CLASSPATH = "dependency:build-classpath";

    public static String DEPS_FILENAME = "module";

    public static String CLASSPATH_EXT = ".cpath";

    public static String MAVEN_DEP_PLUGING_OUTPUT_FILE = "-Dmdep.outputFile=";

    public static String MAVEN_DEP_ARG_CLASSPATH = MAVEN_DEP_PLUGING_OUTPUT_FILE + DEPS_FILENAME + CLASSPATH_EXT;

    public static String MAVEN_PLUGIN_CONFIGURATION = "configuration";

    public static String MAVEN_COMPILER_ID = "compilerId";

    public static String MAVEN_SKIP = "skip";

    public static String MAVEN_SOURCE = "source";

    public static String MAVEN_TARGET = "target";

    public static String FAIL_ON_ERROR = "failOnError";

    public static String MAVEN_SKIP_MAIN = "skipMain";

    public static String MAVEN_DEFAULT_COMPILE = "default-compile";

    public static String MAVEN_PHASE_NONE = "none";

    public static String COMPILATION_ID = "compilation.ID";
}