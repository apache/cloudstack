package org.apache.cloudstack.iam.server.dao;

import java.util.List;

import org.apache.cloudstack.iam.server.IAMGroupPolicyMapVO;
import com.cloud.utils.db.GenericDao;

public interface IAMGroupPolicyMapDao extends GenericDao<IAMGroupPolicyMapVO, Long> {

    List<IAMGroupPolicyMapVO> listByGroupId(long groupId);

    List<IAMGroupPolicyMapVO> listByPolicyId(long policyId);

    IAMGroupPolicyMapVO findByGroupAndPolicy(long groupId, long policyId);

}
