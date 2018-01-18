package org.kie.workbench.common.services.backend.compiler.impl.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import org.junit.Test;

public class MapClassLoaderTest {

    @Test
    public void test(){
        MapClassLoader EMPTY_CLASSLOADER = new MapClassLoader(Collections.emptyMap(), new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader().getParent()));

    }

}
