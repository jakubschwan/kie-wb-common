package org.kie.workbench.common.services.backend.builder.cache;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.guvnor.common.services.backend.cache.Cache;
import org.guvnor.common.services.backend.cache.LRUCache;
import org.guvnor.common.services.backend.file.FileDiscoveryService;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.m2repo.backend.server.GuvnorM2Repository;
import org.kie.soup.project.datamodel.commons.util.MVELEvaluator;
import org.kie.workbench.common.services.backend.compiler.impl.utils.MavenUtils;
import org.kie.workbench.common.services.datamodel.spi.DataModelExtension;
import org.kie.workbench.common.services.shared.project.KieProject;
import org.kie.workbench.common.services.shared.project.ProjectImportsService;
import org.kie.workbench.common.services.shared.whitelist.PackageNameWhiteListService;
import org.uberfire.io.IOService;

import static org.guvnor.m2repo.backend.server.repositories.ArtifactRepositoryService.GLOBAL_M2_REPO_NAME;

@ApplicationScoped
public class ProjectCache {

    @Inject
    private GuvnorM2Repository guvnorM2Repository;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private ProjectImportsService importsService;

    @Inject
    private PackageNameWhiteListService packageNameWhiteListService;

    @Inject
    private FileDiscoveryService fileDiscoveryService;

    @Inject
    private Instance<DataModelExtension> dataModelExtensionsProvider;

    @Inject
    private MVELEvaluator evaluator;

    //this is always global today.
    // BUT when switching to workspaces.. this structure will change to Map<Workspace, LRUCache<Project, ProjectBuildData>>
    private final Cache<Project, ProjectBuildData> internalCache = new LRUCache<Project, ProjectBuildData>() {
    };

    public Optional<ProjectBuildData> getEntry(Project project) {
        return Optional.ofNullable(internalCache.getEntry(project));
    }

    public ProjectBuildData getOrCreateEntry(Project project) {
        return getEntry(project).orElseGet(() -> {
            //the use of this maven repo needs to be improved for workspaces.. as Workspace will hold the maven repo
            final ProjectBuildData value = new ProjectBuildData(ioService,
                                                                importsService,
                                                                packageNameWhiteListService,
                                                                fileDiscoveryService,
                                                                dataModelExtensionsProvider,
                                                                evaluator,
                                                                (KieProject) project,
                                                                MavenUtils.getMavenRepoDir(guvnorM2Repository.getM2RepositoryDir(GLOBAL_M2_REPO_NAME)));
            internalCache.setEntry(project, value);
            return value;
        });
    }
}
