package com.cloud.tungsten;

import com.cloud.domain.Domain;
import com.cloud.network.TungstenProvider;
import com.cloud.utils.exception.CloudRuntimeException;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.types.Project;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

@Component
public class TungstenProjectManagerImpl extends TungstenResourceManager implements TungstenProjectManager {

    private static final Logger s_logger = Logger.getLogger(TungstenProjectManagerImpl.class);

    @Inject
    TungstenDomainManager tungstenDomainManager;

    @Override
    public void createProjectInTungsten(TungstenProvider tungstenProvider, String projectUuid, String projectName, Domain domain) {
        try {
            ApiConnector _api = getApiConnector(tungstenProvider);
            //check if the project already exists in tungsten
            if(_api.findById(Project.class, projectUuid) != null)
                return;
            //Create tungsten project
            Project tungstenProject = new Project();
            tungstenProject.setDisplayName(projectName);
            tungstenProject.setName(projectName);
            tungstenProject.setUuid(projectUuid);
            net.juniper.tungsten.api.types.Domain tungstenDomain;

            if (domain == null)
                tungstenDomain = tungstenDomainManager.getDefaultTungstenDomain(tungstenProvider);
            else {
                tungstenDomain = tungstenDomainManager.getTungstenDomainByUuid(tungstenProvider, domain.getUuid());
                if (tungstenDomain == null)
                    tungstenDomainManager.createDomainInTungsten(tungstenProvider, domain.getName(), domain.getUuid());
            }
            tungstenProject.setParent(tungstenDomain);
            _api.create(tungstenProject);
        } catch (IOException e) {
            s_logger.error("Failed creating project resource in tungsten.");
            throw new CloudRuntimeException("Failed creating project resource in tungsten.");
        }
    }

    public void deleteProjectFromTungsten(TungstenProvider tungstenProvider, String projectUuid) {
        ApiConnector _api = getApiConnector(tungstenProvider);
        try {
            Project project = (Project) _api.findById(Project.class, projectUuid);
            if (project != null)
                _api.delete(Project.class, projectUuid);
        } catch (IOException e) {
            s_logger.error("Failed deleting project resource from tungsten.");
            throw new CloudRuntimeException("Failed deleting project resource from tungsten.");
        }
    }
}
