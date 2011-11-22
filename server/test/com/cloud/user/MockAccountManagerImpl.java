package com.cloud.user;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.api.commands.CreateAccountCmd;
import com.cloud.api.commands.CreateDomainCmd;
import com.cloud.api.commands.CreateUserCmd;
import com.cloud.api.commands.DeleteAccountCmd;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.DisableAccountCmd;
import com.cloud.api.commands.DisableUserCmd;
import com.cloud.api.commands.EnableAccountCmd;
import com.cloud.api.commands.EnableUserCmd;
import com.cloud.api.commands.LockUserCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateResourceCountCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.server.Criteria;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;


@Local(value = { AccountManager.class, AccountService.class })
public class MockAccountManagerImpl implements Manager, AccountManager {

    @Override
    public UserAccount createAccount(CreateAccountCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteUserAccount(DeleteAccountCmd cmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserAccount disableUser(DisableUserCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount enableUser(EnableUserCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount lockUser(LockUserCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount updateUser(UpdateUserCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account disableAccount(DisableAccountCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account enableAccount(EnableAccountCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account lockAccount(DisableAccountCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account updateAccount(UpdateAccountCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceLimit updateResourceLimit(String accountName, Long domainId, int typeId, Long max) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends ResourceCount> updateResourceCount(UpdateResourceCountCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends ResourceLimit> searchForLimits(Long id, String accountName, Long domainId, Integer type, Long startIndex, Long pageSizeVal) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getSystemAccount() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getSystemUser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteUser(DeleteUserCmd deleteUserCmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAdmin(short accountType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account finalizeOwner(Account caller, String accountName, Long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<String, Long> finalizeAccountDomainForList(Account caller, String accountName, Long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getActiveAccount(String accountName, Long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getActiveAccount(Long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getAccount(Long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getActiveUser(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getUser(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain getDomain(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRootAdmin(short accountType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public User getActiveUserByRegistrationToken(String registrationToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void markUserRegistered(long userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<Long> getDomainParentIds(long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Long> getDomainChildrenIds(String parentDomainPath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findCorrectResourceLimit(long accountId, ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long findCorrectResourceLimit(DomainVO domain, ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long updateAccountResourceCount(long accountId, ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long updateDomainResourceCount(long domainId, ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta) {
        // TODO Auto-generated method stub

    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean resourceLimitExceeded(Account account, ResourceType type, long... count) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getResourceCount(AccountVO account, ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteAccount(AccountVO account, long callerUserId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void checkAccess(Account account, Domain domain) throws PermissionDeniedException {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserVO createUser(CreateUserCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkAccess(Account account, AccessType accessType, ControlledEntity... entities) throws PermissionDeniedException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Domain createDomain(CreateDomainCmd cmd) {
        return null;
    }

}
