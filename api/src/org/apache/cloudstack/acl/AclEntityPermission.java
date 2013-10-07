package org.apache.cloudstack.acl;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.InternalIdentity;

public interface AclEntityPermission extends InternalIdentity {

    Long getAclGroupId();

    String getEntityType();

    Long getEntityId();

    AccessType getAccessType();

    boolean isAllowed();
}
