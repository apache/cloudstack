package org.apache.cloudstack.affinity;

import org.apache.cloudstack.deploy.UserPreferrenceProcessor;

public interface AffinityGroupProcessor extends UserPreferrenceProcessor {

    /**
     * getType() should return the affinity/anti-affinity group being
     * implemented
     *
     * @return String Affinity/Anti-affinity type
     */
    String getType();
}