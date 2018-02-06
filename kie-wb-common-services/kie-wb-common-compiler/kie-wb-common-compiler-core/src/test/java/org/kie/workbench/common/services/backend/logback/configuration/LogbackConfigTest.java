package org.kie.workbench.common.services.backend.logback.configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenConfig;
import org.kie.workbench.common.services.backend.logback.appender.KieSiftingAppender;
import org.slf4j.LoggerFactory;

public class LogbackConfigTest {

    LoggerContext loggerContext = new LoggerContext();


    @Test
    public void configureLoggingProgrammatically() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        LogbackConfig config = new LogbackConfig();
        config.configure(loggerContext);
        root.info("test appender");
        Appender<ILoggingEvent> kieSift =  root.getAppender("KieSift");
        Assert.assertNotNull(kieSift);
        KieSiftingAppender kieSiftAppender = (KieSiftingAppender) kieSift;
        Assert.assertNotNull(kieSiftAppender);
        Assert.assertNotNull(kieSiftAppender.getDiscriminator());
        Assert.assertEquals(kieSiftAppender.getDiscriminatorKey(),MavenConfig.COMPILATION_ID);
        Appender<ILoggingEvent> consoleAppenderGeneric = root.getAppender("consoleAppender");
        ConsoleAppender consoleAppender = (ConsoleAppender) consoleAppenderGeneric;
        Assert.assertNotNull(consoleAppender);
        Encoder enc = consoleAppender.getEncoder();
        PatternLayoutEncoder encoder = (PatternLayoutEncoder) enc;
        Assert.assertEquals(encoder.getPattern(),"%d [%thread] %level %logger{35} - %msg%n");
    }

}
