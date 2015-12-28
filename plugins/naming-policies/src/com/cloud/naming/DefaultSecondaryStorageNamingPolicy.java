package com.cloud.naming;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;

public class DefaultSecondaryStorageNamingPolicy extends AbstractResourceNamingPolicy implements SecondaryStorageVMNamingPolicy {

    @Inject
    private ConfigurationDao _configDao;
    private static final Logger s_logger = Logger.getLogger(DefaultSecondaryStorageNamingPolicy.class);

    String instanceName;

    @PostConstruct
    public void init() {
        Map<String, String> configs = _configDao.getConfiguration("management-server", new HashMap<String, Object>());
        instanceName = configs.get("instance.name");
    }

    @Override
    public void finalizeIdentifiers(SecondaryStorageVmVO vo) {
        if (!checkUuidSimple(vo.getUuid(), SecondaryStorageVm.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, SecondaryStorageVm.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(SecondaryStorageVm.class, resourceId, userId, customUuid);
    }

    @Override
    public String getSsvmName(Long vmId) {
        StringBuilder builder = new StringBuilder("s");
        builder.append(SEPARATOR).append(vmId).append(SEPARATOR).append("instanceName");
        return builder.toString();
    }


}
