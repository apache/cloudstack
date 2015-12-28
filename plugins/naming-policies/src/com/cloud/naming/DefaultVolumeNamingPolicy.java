package com.cloud.naming;

import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;


public class DefaultVolumeNamingPolicy extends AbstractResourceNamingPolicy implements VolumeNamingPolicy {

    private static final Logger s_logger = Logger.getLogger(DefaultVolumeNamingPolicy.class);

    @Override
    public void finalizeIdentifiers(VolumeVO vo) {
        if (!checkUuidSimple(vo.getUuid(), Volume.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, Volume.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(Volume.class, resourceId, userId, customUuid);
    }


    @Override
    public String getDatadiskName() {
        return UUID.randomUUID().toString();
    }


    @Override
    public String getDatadiskName(long instanceId) {
        return "DATA-" + instanceId;
    }


    @Override
    public String getRootName(Long instanceId) {
        return "ROOT-" + instanceId;
    }

}
