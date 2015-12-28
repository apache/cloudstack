package com.cloud.naming;

import org.apache.cloudstack.api.Identity;

public interface ResourceNamingPolicy <V extends Identity, VO extends V> {

    public void checkCustomUuid(String uuid);

    public String generateUuid(Long resourceId, Long accountId, String customUuid);


    /**
     * Verifies that the uuid and various other names are
     * unique when necessary and compliant with the naming policies.
     * Generated new identifiers when the provided ones are invalid.
     * @param vo
     */
    public void finalizeIdentifiers(VO vo);

}
