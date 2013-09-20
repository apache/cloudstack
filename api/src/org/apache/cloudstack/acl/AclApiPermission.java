package org.apache.cloudstack.acl;

import org.apache.cloudstack.api.InternalIdentity;

public interface AclApiPermission extends InternalIdentity {

    Long getAclRoleId();

    String getApiName();

}
