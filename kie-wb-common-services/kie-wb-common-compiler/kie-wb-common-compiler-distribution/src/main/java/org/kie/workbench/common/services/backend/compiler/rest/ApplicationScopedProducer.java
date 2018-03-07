package org.kie.workbench.common.services.backend.compiler.rest;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.service.AuthenticationService;

import org.uberfire.commons.services.cdi.Startup;
import org.uberfire.commons.services.cdi.StartupType;


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

    /*@Inject
    private IOWatchServiceAllImpl watchService;

    private IOService ioService;

    @PostConstruct
    public void setup() {
        ioService = new IOServiceNio2WrapperImpl("1",
                                                 watchService);
    }

    @Produces
    @Named("ioStrategy")
    public IOService ioService() {
        return ioService;
    }*/
}

