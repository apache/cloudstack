package com.cloud.tungsten;

import com.cloud.network.TungstenProvider;
import com.cloud.utils.exception.CloudRuntimeException;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.Project;
import org.apache.log4j.Logger;

import java.io.IOException;

public class TungstenDomainManagerImpl extends TungstenResourceManager implements TungstenDomainManager {

    private static final Logger s_logger = Logger.getLogger(TungstenDomainManagerImpl.class);

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";

    public void createDomainInTungsten(TungstenProvider tungstenProvider, String domainName, String domainUuid){
        try {
            ApiConnector _api = getApiConnector(tungstenProvider);
            //check if the domain exists in tungsten
            if(_api.findById(Domain.class, domainUuid) != null)
                return;
            //create tungsten domain
            Domain tungstenDomain = new Domain();
            tungstenDomain.setDisplayName(domainName);
            tungstenDomain.setName(domainName);
            tungstenDomain.setUuid(domainUuid);
            _api.create(tungstenDomain);
            // create default project in tungsten for this newly created domain
            Project tungstenDefaultProject = new Project();
            tungstenDefaultProject.setDisplayName(TUNGSTEN_DEFAULT_PROJECT);
            tungstenDefaultProject.setName(TUNGSTEN_DEFAULT_PROJECT);
            tungstenDefaultProject.setParent(tungstenDomain);
            _api.create(tungstenDefaultProject);
        } catch (IOException e) {
            s_logger.error("Failed creating domain resource in tungsten.");
            throw new CloudRuntimeException("Failed creating domain resource in tungsten.");
        }
    }

    public void deleteDomainFromTungsten(TungstenProvider tungstenProvider, String domainUuid){
        try {
            ApiConnector _api = getApiConnector(tungstenProvider);
            Domain domain = (Domain) _api.findById(Domain.class, domainUuid);
            //delete the projects of this domain
            for(ObjectReference<ApiPropertyBase> project : domain.getProjects()){
                _api.delete(Project.class, project.getUuid());
            }
            _api.delete(Domain.class, domainUuid);
        } catch (IOException e) {
            s_logger.error("Failed deleting domain resource from tungsten.");
            throw new CloudRuntimeException("Failed deleting domain resource from tungsten.");
        }
    }

    public Domain getDefaultTungstenDomain(TungstenProvider tungstenProvider) throws IOException {
        ApiConnector _api = getApiConnector(tungstenProvider);
        Domain domain = (Domain) _api.findByFQN(Domain.class, TUNGSTEN_DEFAULT_DOMAIN);
        return domain;
    }

    public Domain getTungstenDomainByUuid(TungstenProvider tungstenProvider, String domainUuid) throws IOException {
        ApiConnector _api = getApiConnector(tungstenProvider);
        return (Domain) _api.findById(Domain.class, domainUuid);
    }
}
