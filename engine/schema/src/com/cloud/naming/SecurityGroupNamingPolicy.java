package com.cloud.naming;

import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupVO;

public interface SecurityGroupNamingPolicy extends ResourceNamingPolicy<SecurityGroup, SecurityGroupVO> {

    public String getSgDefaultName();
}
