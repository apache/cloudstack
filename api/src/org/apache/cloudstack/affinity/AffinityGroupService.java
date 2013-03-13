package org.apache.cloudstack.affinity;

import java.util.List;

import com.cloud.exception.ResourceInUseException;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

public interface AffinityGroupService {

    /**
     * Creates an affinity/anti-affinity group for the given account/domain.
     *
     * @param account
     * @param domainId
     * @param name
     * @param type
     * @param description
     * @return AffinityGroup
     */

    AffinityGroup createAffinityGroup(String account, Long domainId, String affinityGroupName,
            String affinityGroupType, String description);

    /**
     * Creates an affinity/anti-affinity group.
     *
     * @param affinityGroupId
     * @param account
     * @param domainId
     * @param affinityGroupName
     * @throws ResourceInUseException
     */
    boolean deleteAffinityGroup(Long affinityGroupId, String account, Long domainId, String affinityGroupName)
            throws ResourceInUseException;

    /** Lists Affinity Groups in your account
     * @param account
     * @param domainId
     * @param affinityGroupId
     * @param affinityGroupName
     * @param affinityGroupType
     * @param vmId
     * @param startIndex
     * @param pageSize
     * @return
     */
    Pair<List<? extends AffinityGroup>, Integer> listAffinityGroups(Long affinityGroupId, String affinityGroupName,
            String affinityGroupType, Long vmId, Long startIndex, Long pageSize);


    /**
     * List group types available in deployment
     *
     * @return
     */
    List<String> listAffinityGroupTypes();

    AffinityGroup getAffinityGroup(Long groupId);

    UserVm updateVMAffinityGroups(Long vmId, List<Long> affinityGroupIds);

}
