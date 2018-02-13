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

import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

import org.guvnor.common.services.project.backend.server.utils.configuration.ConfigurationKey;
import org.guvnor.common.services.project.backend.server.utils.configuration.ConfigurationStrategy;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void loadConfig() {
        ConfigurationContextProvider provider = new ConfigurationContextProvider();
        Map<ConfigurationKey, String> conf = provider.loadConfiguration();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadStaticConfig() {
        ConfigurationStrategy strategy = new ConfigurationStaticStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadPropertiesConfig() {
        ConfigurationStrategy strategy = new ConfigurationPropertiesStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadEnvironmentConfig() {
        ConfigurationStrategy strategy = new ConfigurationEnvironmentStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(conf).isEmpty();

        strategy = new ConfigurationEnvironmentStrategy(getMapForEnv());
        conf = strategy.loadConfiguration();
        assertThat(conf.isEmpty()).isFalse();
        assertThat(conf.keySet()).hasSize(14);
    }

    private Map<String, String> getMapForEnv() {
        Map conf = new HashMap<>();
        conf.put(ConfigurationKey.COMPILER.name(), "jdt");
        conf.put(ConfigurationKey.SOURCE_VERSION.name(), "1.8");
        conf.put(ConfigurationKey.TARGET_VERSION.name(), "1.8");
        conf.put(ConfigurationKey.MAVEN_COMPILER_PLUGIN_GROUP.name(), "org.apache.maven.plugins");
        conf.put(ConfigurationKey.MAVEN_COMPILER_PLUGIN_ARTIFACT.name(), "maven-compiler-plugin");
        conf.put(ConfigurationKey.MAVEN_COMPILER_PLUGIN_VERSION.name(), "3.6.1");
        conf.put(ConfigurationKey.FAIL_ON_ERROR.name(), "false");
        conf.put(ConfigurationKey.TAKARI_COMPILER_PLUGIN_GROUP.name(), "kie.io.takari.maven.plugins");
        conf.put(ConfigurationKey.TAKARI_COMPILER_PLUGIN_ARTIFACT.name(), "kie-takari-lifecycle-plugin");
        conf.put(ConfigurationKey.TAKARI_COMPILER_PLUGIN_VERSION.name(), "1.13.3");
        conf.put(ConfigurationKey.KIE_MAVEN_PLUGINS.name(), "org.kie");
        conf.put(ConfigurationKey.KIE_MAVEN_PLUGIN.name(), "kie-maven-plugin");
        conf.put(ConfigurationKey.KIE_TAKARI_PLUGIN.name(), "kie-takari-plugin");
        conf.put(ConfigurationKey.KIE_VERSION.name(), "7.6.0-SNAPSHOT");
        return conf;
    }
}
