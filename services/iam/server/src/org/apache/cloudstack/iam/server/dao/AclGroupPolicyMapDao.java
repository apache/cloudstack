package org.apache.cloudstack.iam.server.dao;

import java.util.List;

import org.apache.cloudstack.iam.server.AclGroupPolicyMapVO;
import com.cloud.utils.db.GenericDao;

public interface AclGroupPolicyMapDao extends GenericDao<AclGroupPolicyMapVO, Long> {

    List<AclGroupPolicyMapVO> listByGroupId(long groupId);

    List<AclGroupPolicyMapVO> listByPolicyId(long policyId);

    AclGroupPolicyMapVO findByGroupAndPolicy(long groupId, long policyId);

}
