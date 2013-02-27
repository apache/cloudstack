package org.apache.cloudstack.affinity;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.log4j.Logger;


import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;

@Local(value = { AffinityGroupService.class })
public class AffinityGroupServiceImpl extends ManagerBase implements AffinityGroupService, Manager {

    public static final Logger s_logger = Logger.getLogger(AffinityGroupServiceImpl.class);
    private String _name;

    @Inject
    AccountManager _accountMgr;

    @Inject
    AffinityGroupDao _affinityGroupDao;

    @Override
    public AffinityGroup createAffinityGroup(String account, Long domainId, String affinityGroupName,
            String affinityGroupType, String description) {

        Account caller = UserContext.current().getCaller();
        Account owner = _accountMgr.finalizeOwner(caller, account, domainId, null);

        if (_affinityGroupDao.isNameInUse(owner.getId(), owner.getDomainId(), affinityGroupName)) {
            throw new InvalidParameterValueException("Unable to create affinity group, a group with name "
                    + affinityGroupName
                    + " already exisits.");
        }

        AffinityGroupVO group = new AffinityGroupVO(affinityGroupName, affinityGroupType, description, domainId,
                owner.getId());
        _affinityGroupDao.persist(group);

        return group;
    }

    @Override
    public void deleteAffinityGroup(Long affinityGroupId, String account, Long domainId, String affinityGroupName,
            Long vmId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<AffinityGroup> listAffinityGroups(String account, Long domainId, Long affinityGroupId,
            String affinityGroupName, String affinityGroupType, Long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listAffinityGroupTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

}
