package org.kie.workbench.common.services.backend.compiler.configuration;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.guvnor.common.services.project.backend.server.utils.configuration.ConfigurationKey;
import org.guvnor.common.services.project.backend.server.utils.configuration.ConfigurationStrategy;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ConfigurationTest {


    @Test
    public void loadConfig(){
        ConfigurationContextProvider provider = new ConfigurationContextProvider();
        Map<ConfigurationKey, String> conf = provider.loadConfiguration();
        Assert.assertTrue(conf.keySet().size() ==  14);
    }


    @Test
    public void loadStaticConfig(){
        ConfigurationStrategy strategy = new ConfigurationStaticStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        Assert.assertTrue(conf.keySet().size() ==  14);
    }


    @Test
    public void loadPropertiesConfig(){
        ConfigurationStrategy strategy = new ConfigurationPropertiesStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        Assert.assertTrue(conf.keySet().size() ==  14);
    }


    @Test @Ignore
    public void loadEnvironmentConfig() throws  Exception{
        ConfigurationStrategy strategy = new ConfigurationEnvironmentStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        Assert.assertFalse(conf.keySet().size() ==  14);

        setEnv(getMapForEnv());
        strategy = new ConfigurationEnvironmentStrategy();
        conf = strategy.loadConfiguration();
        Assert.assertTrue(conf.keySet().size() ==  14);
    }

    private Map<String, String> getMapForEnv(){
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
        return  conf;
    }


    protected static void setEnv(Map<String, String> newEnv) throws Exception {

    }



}
