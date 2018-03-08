package org.kie.workbench.common.services.backend.compiler.rest;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.service.AuthenticationService;

import org.uberfire.backend.server.IOWatchServiceAllImpl;
import org.uberfire.commons.services.cdi.Startup;
import org.uberfire.commons.services.cdi.StartupType;
import org.uberfire.io.IOService;
import org.uberfire.io.impl.IOServiceNio2WrapperImpl;

@Startup(value = StartupType.BOOTSTRAP)
@ApplicationScoped
public class ApplicationScopedProducer {

    @Inject
    private AuthenticationService authenticationService;


    @Produces
    @RequestScoped
    public User getIdentity() {
        return authenticationService.getUser();
    }

}

