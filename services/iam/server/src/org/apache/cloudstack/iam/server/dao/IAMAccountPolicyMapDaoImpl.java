package org.apache.cloudstack.iam.server.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.iam.server.IAMAccountPolicyMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class IAMAccountPolicyMapDaoImpl extends GenericDaoBase<IAMAccountPolicyMapVO, Long> implements IAMAccountPolicyMapDao {

    private SearchBuilder<IAMAccountPolicyMapVO> ListByAccountId;
    private SearchBuilder<IAMAccountPolicyMapVO> ListByPolicyId;
    private SearchBuilder<IAMAccountPolicyMapVO> findByPolicyAccountId;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        ListByAccountId = createSearchBuilder();
        ListByAccountId.and("accountId", ListByAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListByAccountId.done();

        ListByPolicyId = createSearchBuilder();
        ListByPolicyId.and("policyId", ListByPolicyId.entity().getIAMPolicyId(), SearchCriteria.Op.EQ);
        ListByPolicyId.done();

        findByPolicyAccountId = createSearchBuilder();
        findByPolicyAccountId.and("policyId", findByPolicyAccountId.entity().getIAMPolicyId(), SearchCriteria.Op.EQ);
        findByPolicyAccountId.and("accountId", findByPolicyAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        findByPolicyAccountId.done();

        return true;
    }

    @Override
    public List<IAMAccountPolicyMapVO> listByAccountId(long acctId) {
        SearchCriteria<IAMAccountPolicyMapVO> sc = ListByAccountId.create();
        sc.setParameters("accountId", acctId);
        return listBy(sc);
    }

    @Override
    public List<IAMAccountPolicyMapVO> listByPolicyId(long policyId) {
        SearchCriteria<IAMAccountPolicyMapVO> sc = ListByPolicyId.create();
        sc.setParameters("policyId", policyId);
        return listBy(sc);
    }

    @Override
    public IAMAccountPolicyMapVO findByAccountAndPolicy(long acctId, long policyId) {
        SearchCriteria<IAMAccountPolicyMapVO> sc = findByPolicyAccountId.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("accountId", acctId);
        return findOneBy(sc);
    }
}