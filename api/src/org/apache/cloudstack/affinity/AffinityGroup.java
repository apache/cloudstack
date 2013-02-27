package org.apache.cloudstack.affinity;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface AffinityGroup extends ControlledEntity, InternalIdentity, Identity {

    String getName();

    String getDescription();

    String getType();

}
