package org.apache.cloudstack.iam.server.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.iam.server.AclAccountPolicyMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class AclAccountPolicyMapDaoImpl extends GenericDaoBase<AclAccountPolicyMapVO, Long> implements AclAccountPolicyMapDao {

    private SearchBuilder<AclAccountPolicyMapVO> ListByAccountId;
    private SearchBuilder<AclAccountPolicyMapVO> ListByPolicyId;
    private SearchBuilder<AclAccountPolicyMapVO> findByPolicyAccountId;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        ListByAccountId = createSearchBuilder();
        ListByAccountId.and("accountId", ListByAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListByAccountId.done();

        ListByPolicyId = createSearchBuilder();
        ListByPolicyId.and("policyId", ListByPolicyId.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        ListByPolicyId.done();

        findByPolicyAccountId = createSearchBuilder();
        findByPolicyAccountId.and("policyId", findByPolicyAccountId.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        findByPolicyAccountId.and("accountId", findByPolicyAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        findByPolicyAccountId.done();

        return true;
    }

    @Override
    public List<AclAccountPolicyMapVO> listByAccountId(long acctId) {
        SearchCriteria<AclAccountPolicyMapVO> sc = ListByAccountId.create();
        sc.setParameters("accountId", acctId);
        return listBy(sc);
    }

    @Override
    public List<AclAccountPolicyMapVO> listByPolicyId(long policyId) {
        SearchCriteria<AclAccountPolicyMapVO> sc = ListByPolicyId.create();
        sc.setParameters("policyId", policyId);
        return listBy(sc);
    }

    @Override
    public AclAccountPolicyMapVO findByAccountAndPolicy(long acctId, long policyId) {
        SearchCriteria<AclAccountPolicyMapVO> sc = findByPolicyAccountId.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("accountId", acctId);
        return findOneBy(sc);
    }
}