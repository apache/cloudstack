package org.apache.cloudstack.iam.server.dao;

import java.util.List;

import org.apache.cloudstack.iam.server.AclAccountPolicyMapVO;

import com.cloud.utils.db.GenericDao;

public interface AclAccountPolicyMapDao extends GenericDao<AclAccountPolicyMapVO, Long> {

    List<AclAccountPolicyMapVO> listByAccountId(long acctId);

    List<AclAccountPolicyMapVO> listByPolicyId(long policyId);

    AclAccountPolicyMapVO findByAccountAndPolicy(long acctId, long policyId);

}
