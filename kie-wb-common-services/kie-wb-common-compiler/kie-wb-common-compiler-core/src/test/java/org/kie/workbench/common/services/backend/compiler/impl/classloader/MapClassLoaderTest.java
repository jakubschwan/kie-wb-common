package org.kie.workbench.common.services.backend.compiler.impl.classloader;

import org.drools.core.util.ClassUtils;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class MapClassLoaderTest {
    @Test
    public void testCanonicalNameSimpleClass() {
        String name = ClassUtils.canonicalName(MapClassLoaderTest.class );
        assertEquals( "org.kie.workbench.common.services.backend.compiler.impl.classloader.MapClassLoaderTest",
                      name );
    }

    @Test
    public void testCanonicalNameInnerClass() {
        String name = ClassUtils.canonicalName( A.class );
        assertEquals( "org.kie.workbench.common.services.backend.compiler.impl.classloader.MapClassLoaderTest.A",
                      name );
    }

    @Test
    public void testCanonicalNameInnerInnerClass() {
        String name = ClassUtils.canonicalName( A.B.class );
        assertEquals( "org.kie.workbench.common.services.backend.compiler.impl.classloader.MapClassLoaderTest.A.B",
                      name );
    }

    @Test
    public void testCanonicalNameArray() {
        String name = ClassUtils.canonicalName( Object[].class );
        assertEquals( "java.lang.Object[]",
                      name );
    }

    @Test
    public void testCanonicalNameMultiIndexArray() {
        String name = ClassUtils.canonicalName( Object[][][].class );
        assertEquals( "java.lang.Object[][][]",
                      name );
    }

    @Test
    public void testCanonicalNameMultiIndexArrayInnerClass() {
        String name = ClassUtils.canonicalName( A.B[][][].class );
        assertEquals( "org.kie.workbench.common.services.backend.compiler.impl.classloader.MapClassLoaderTest.A.B[][][]",
                      name );
    }

    @Test
    public void testCanonicalNameMultiIndexArrayPrimitives() {
        String name = ClassUtils.canonicalName( long[][][].class );
        assertEquals( "long[][][]",
                      name );
    }

    public static class A {
        public static class B {
        }
    }

}
