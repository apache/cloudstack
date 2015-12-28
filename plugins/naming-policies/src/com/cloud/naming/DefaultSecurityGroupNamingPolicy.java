package com.cloud.naming;

import org.apache.log4j.Logger;

import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupVO;

public class DefaultSecurityGroupNamingPolicy extends AbstractResourceNamingPolicy implements SecurityGroupNamingPolicy {

    private static final String DEFAULT_GROUP_NAME = "default";
    private static final Logger s_logger = Logger.getLogger(DefaultSecurityGroupNamingPolicy.class);

    @Override
    public void finalizeIdentifiers(SecurityGroupVO vo) {
        if (!checkUuidSimple(vo.getUuid(), SecurityGroup.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, SecurityGroup.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(SecurityGroup.class, resourceId, userId, customUuid);
    }

    @Override
    public String getSgDefaultName() {
        return DEFAULT_GROUP_NAME;
    }

}