package org.apache.cloudstack.iam.server.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.iam.server.AclGroupPolicyMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class AclGroupPolicyMapDaoImpl extends GenericDaoBase<AclGroupPolicyMapVO, Long> implements AclGroupPolicyMapDao {

    private SearchBuilder<AclGroupPolicyMapVO> ListByGroupId;
    private SearchBuilder<AclGroupPolicyMapVO> ListByPolicyId;
    private SearchBuilder<AclGroupPolicyMapVO> findByPolicyGroupId;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        ListByGroupId = createSearchBuilder();
        ListByGroupId.and("groupId", ListByGroupId.entity().getAclGroupId(), SearchCriteria.Op.EQ);
        ListByGroupId.done();

        ListByPolicyId = createSearchBuilder();
        ListByPolicyId.and("policyId", ListByPolicyId.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        ListByPolicyId.done();

        findByPolicyGroupId = createSearchBuilder();
        findByPolicyGroupId.and("policyId", findByPolicyGroupId.entity().getAclPolicyId(), SearchCriteria.Op.EQ);
        findByPolicyGroupId.and("groupId", findByPolicyGroupId.entity().getAclGroupId(), SearchCriteria.Op.EQ);
        findByPolicyGroupId.done();

        return true;
    }

    @Override
    public List<AclGroupPolicyMapVO> listByGroupId(long groupId) {
        SearchCriteria<AclGroupPolicyMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }

    @Override
    public List<AclGroupPolicyMapVO> listByPolicyId(long policyId) {
        SearchCriteria<AclGroupPolicyMapVO> sc = ListByPolicyId.create();
        sc.setParameters("policyId", policyId);
        return listBy(sc);
    }

    @Override
    public AclGroupPolicyMapVO findByGroupAndPolicy(long groupId, long policyId) {
        SearchCriteria<AclGroupPolicyMapVO> sc = findByPolicyGroupId.create();
        sc.setParameters("policyId", policyId);
        sc.setParameters("groupId", groupId);
        return findOneBy(sc);
    }
}