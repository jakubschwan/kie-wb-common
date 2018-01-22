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
package org.kie.workbench.common.services.backend.logback.appender;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UUIDThreadNameDiscriminatorTest {

    private Logger logger = LoggerFactory.getLogger(UUIDThreadNameDiscriminatorTest.class);

    @Test
    public void uuidThreadNameDiscriminatorTest(){
        String compilationID ="80dbc168-a1fe-499d-a414-d4a37d13d100-1516620517065-1";
        String[] args = new String[]{};
        LoggingEvent event = new LoggingEvent("org.kie.workbench.common.services.backend.logback.appender.KieSiftingAppenderTest",
                                              (ch.qos.logback.classic.Logger)logger,
                                              Level.INFO,
                                              "I'm a beatiful test message :)",
                                              null, args);
        Map<String,String> mdcMap = new HashMap<>();
        mdcMap.put(MavenConfig.COMPILATION_ID, compilationID);
        event.setMDCPropertyMap(mdcMap);

        UUIDThreadNameDiscriminator discriminator = new UUIDThreadNameDiscriminator();
        discriminator.start();
        Assert.assertTrue(discriminator.isStarted());
        Assert.assertTrue(discriminator.getKey().equals(MavenConfig.COMPILATION_ID));
        String threadName = Thread.currentThread().getName();
        Assert.assertTrue(discriminator.getDiscriminatingValue(event).equals(threadName));
        discriminator.stop();
        Assert.assertFalse(discriminator.isStarted());
    }

}
