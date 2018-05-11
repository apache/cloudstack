package org.apache.cloudstack.framework.br;

import java.util.List;

/**
 * Backup and Recovery Policies Services
 */
public interface BRPolicyService {

    /**
     * Add a new Backup and Recovery policy
     */
    BRPolicy addBRPolicy(String policyId, String policyName, String providerId);

    /**
     * List Backup policies
     */
    List<BRPolicy> listBRPolicies(String providerId);
}
