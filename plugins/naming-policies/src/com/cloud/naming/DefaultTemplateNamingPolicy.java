package com.cloud.naming;

import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateVO;
import com.cloud.template.VirtualMachineTemplate;

public class DefaultTemplateNamingPolicy extends AbstractResourceNamingPolicy implements TemplateNamingPolicy {

    private static final Logger s_logger = Logger.getLogger(DefaultTemplateNamingPolicy.class);

    @Override
    public void finalizeIdentifiers(VMTemplateVO vo) {
        if (!checkUuidSimple(vo.getUuid(), VirtualMachineTemplate.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, VirtualMachineTemplate.class);
    }


    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(VirtualMachineTemplate.class, resourceId, userId, customUuid);
    }


}