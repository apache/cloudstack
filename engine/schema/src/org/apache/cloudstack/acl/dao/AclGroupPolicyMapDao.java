package org.apache.cloudstack.acl.dao;

import java.util.List;

import org.apache.cloudstack.acl.AclGroupPolicyMapVO;

import com.cloud.utils.db.GenericDao;

public interface AclGroupPolicyMapDao extends GenericDao<AclGroupPolicyMapVO, Long> {

    List<AclGroupPolicyMapVO> listByGroupId(long groupId);

    List<AclGroupPolicyMapVO> listByPolicyId(long policyId);

    AclGroupPolicyMapVO findByGroupAndPolicy(long groupId, long policyId);

}
