package org.apache.cloudstack.backup;

import org.apache.cloudstack.api.InternalIdentity;

public interface BackupPolicyVMMap extends InternalIdentity {

    long getPolicyId();
    long getVmId();
    long getZoneId();
}
