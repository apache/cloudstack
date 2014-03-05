package org.apache.cloudstack.iam.server.dao;

import java.util.List;

import org.apache.cloudstack.iam.server.IAMAccountPolicyMapVO;

import com.cloud.utils.db.GenericDao;

public interface IAMAccountPolicyMapDao extends GenericDao<IAMAccountPolicyMapVO, Long> {

    List<IAMAccountPolicyMapVO> listByAccountId(long acctId);

    List<IAMAccountPolicyMapVO> listByPolicyId(long policyId);

    IAMAccountPolicyMapVO findByAccountAndPolicy(long acctId, long policyId);

}
