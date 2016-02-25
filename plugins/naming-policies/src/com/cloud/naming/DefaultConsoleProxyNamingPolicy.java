package com.cloud.naming;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.vm.ConsoleProxy;
import com.cloud.vm.ConsoleProxyVO;

public class DefaultConsoleProxyNamingPolicy extends AbstractResourceNamingPolicy implements ConsoleProxyNamingPolicy {

    @Inject
    private ConfigurationDao _configDao;

    private static final Logger s_logger = Logger.getLogger(DefaultConsoleProxyNamingPolicy.class);
    String instanceName;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration("management-server", new HashMap<String, Object>());
        instanceName = configs.get("instance.name");
        return true;
    }

    @Override
    public void finalizeIdentifiers(ConsoleProxyVO vo) {
        if (!checkUuidSimple(vo.getUuid(), ConsoleProxy.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }


    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, ConsoleProxy.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(ConsoleProxy.class, resourceId, userId, customUuid);
    }

    @Override
    public String getConsoleProxyName(Long resourceId) {
        StringBuilder builder = new StringBuilder("v");
        builder.append(SEPARATOR).append(resourceId).append(SEPARATOR).append(instanceName);
        return builder.toString();
    }


}
