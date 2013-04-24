// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.vpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.NetworkACLItem.State;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLListCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLListsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.firewall.NetworkACLService;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;


@Component
@Local(value = { NetworkACLService.class, NetworkACLManager.class})
public class NetworkACLManagerImpl extends ManagerBase implements NetworkACLManager{
    private static final Logger s_logger = Logger.getLogger(NetworkACLManagerImpl.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    List<NetworkACLServiceProvider> _networkAclElements;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkDao _networkDao;


    @Override
    public boolean revokeACLItemsForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(network.getNetworkACLId());
        if (aclItems.isEmpty()) {
            s_logger.debug("Found no network ACL Items for network id=" + networkId);
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + aclItems.size() + " Network ACL Items for network id=" + networkId);
        }

        for (NetworkACLItemVO aclItem : aclItems) {
            // Mark all Network ACLs rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no
            // need to send them one by one
            //revokeNetworkACLItem(aclItem.getId(), false, caller, Account.ACCOUNT_ID_SYSTEM);
            if (aclItem.getState() == State.Add || aclItem.getState() == State.Active) {
                aclItem.setState(State.Revoke);
            }
        }

        //List<NetworkACLItemVO> ACLsToRevoke = _networkACLItemDao.listByNetwork(networkId);

        // now send everything to the backend
        boolean success = applyACLItemsToNetwork(network.getId(), aclItems, caller);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released Network ACLs for network id=" + networkId + " and # of rules now = "
                    + aclItems.size());
        }

        return success;
    }

    @Override
    public List<NetworkACLItemVO> listNetworkACLItems(long guestNtwkId) {
        Network network = _networkMgr.getNetwork(guestNtwkId);
        return _networkACLItemDao.listByACL(network.getNetworkACLId());
    }

    @Override
    public NetworkACLItem getNetworkACLItem(long ruleId) {
        return _networkACLItemDao.findById(ruleId);
    }

    @Override
    public boolean applyNetworkACL(long aclId, Account caller) throws ResourceUnavailableException {
        boolean handled = false;
        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(aclId);
        //Find all networks using this ACL
        List<NetworkVO> networks = _networkDao.listByAclId(aclId);
        for(NetworkVO network : networks){
            //Failure case??
            handled = applyACLItemsToNetwork(network.getId(), rules, caller);
        }
        if(handled){
            for (NetworkACLItem rule : rules) {
                if (rule.getState() == NetworkACLItem.State.Revoke) {
                    removeRule(rule);
                } else if (rule.getState() == NetworkACLItem.State.Add) {
                    NetworkACLItemVO ruleVO = _networkACLItemDao.findById(rule.getId());
                    ruleVO.setState(NetworkACLItem.State.Active);
                    _networkACLItemDao.update(ruleVO.getId(), ruleVO);
                }
            }
        }
        return handled;
    }

    @Override
    public void removeRule(NetworkACLItem rule) {
        //remove the rule
        _networkACLItemDao.remove(rule.getId());
    }

    @Override
    public boolean applyACLToNetwork(long networkId, Account caller) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(network.getNetworkACLId());
        return applyACLItemsToNetwork(networkId, rules, caller);
    }

    public boolean applyACLItemsToNetwork(long networkId, List<NetworkACLItemVO> rules, Account caller) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        boolean handled = false;
        for (NetworkACLServiceProvider element: _networkAclElements) {
            Network.Provider provider = element.getProvider();
            boolean  isAclProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.NetworkACL, provider);
            if (!isAclProvider) {
                continue;
            }
            handled = element.applyNetworkACLs(network, rules);
            if (handled)
                break;
        }
        return handled;
    }

    @Override
    public NetworkACLItem createNetworkACLItem(CreateNetworkACLCmd aclItemCmd) throws NetworkRuleConflictException {
        return createNetworkACLItem(UserContext.current().getCaller(), aclItemCmd.getSourcePortStart(),
                aclItemCmd.getSourcePortEnd(), aclItemCmd.getProtocol(), aclItemCmd.getSourceCidrList(), aclItemCmd.getIcmpCode(),
                aclItemCmd.getIcmpType(), aclItemCmd.getNetworkId(), aclItemCmd.getTrafficType(), aclItemCmd.getACLId(), aclItemCmd.getAction(), aclItemCmd.getNumber());
    }

    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_ITEM_CREATE, eventDescription = "creating network ACL Item", create = true)
    protected NetworkACLItem createNetworkACLItem(Account caller, Integer portStart, Integer portEnd, String protocol, List<String> sourceCidrList,
                                                  Integer icmpCode, Integer icmpType, Long networkId, NetworkACLItem.TrafficType trafficType, Long aclId,
                                                  String action, Integer number) throws NetworkRuleConflictException {

        if(aclId == null){
            Network network = _networkMgr.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Can't find network by id");
            }
            aclId = network.getNetworkACLId();

            if (aclId == null) {
                throw new InvalidParameterValueException("Network is not associated with any ACL");
            }
        }

        NetworkACL networkACL = _networkACLDao.findById(aclId);

        Vpc vpc = _vpcMgr.getVpc(networkACL.getVpcId());
        Account aclOwner = _accountMgr.getAccount(vpc.getAccountId());

        //check if the caller can access vpc
        _accountMgr.checkAccess(caller, null, false, vpc);

        //check if the acl can be created for this network
        _accountMgr.checkAccess(aclOwner, AccessType.ModifyEntry, false, networkACL);

        // icmp code and icmp type can't be passed in for any other protocol rather than icmp
        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }

        if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        }

        //validate icmp code and type
        if (icmpType != null) {
            if (icmpType.longValue() != -1 && !NetUtils.validateIcmpType(icmpType.longValue())) {
                throw new InvalidParameterValueException("Invalid icmp type; should belong to [0-255] range");
            }
            if (icmpCode != null) {
                if (icmpCode.longValue() != -1 && !NetUtils.validateIcmpCode(icmpCode.longValue())) {
                    throw new InvalidParameterValueException("Invalid icmp code; should belong to [0-15] range and can" +
                            " be defined when icmpType belongs to [0-40] range");
                }
            }
        }

        NetworkACLItem.Action ruleAction = NetworkACLItem.Action.Allow;
        if("deny".equals(action)){
            ruleAction = NetworkACLItem.Action.Deny;
        }
        // If number is null, set it to currentMax + 1
        validateNetworkACLItem(caller, portStart, portEnd, protocol);

        Transaction txn = Transaction.currentTxn();
        txn.start();


        NetworkACLItemVO newRule = new NetworkACLItemVO(portStart, portEnd, protocol.toLowerCase(), aclId, sourceCidrList, icmpCode, icmpType, trafficType, ruleAction, number);
        newRule = _networkACLItemDao.persist(newRule);

            //ToDo: Is this required now with??
            //detectNetworkACLConflict(newRule);

        if (!_networkACLItemDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        UserContext.current().setEventDetails("ACL Item Id: " + newRule.getId());

        txn.commit();

        return getNetworkACLItem(newRule.getId());
    }

    protected void validateNetworkACLItem(Account caller, Integer portStart, Integer portEnd,
                                          String proto) {

        if (portStart != null && !NetUtils.isValidPort(portStart)) {
            throw new InvalidParameterValueException("publicPort is an invalid value: " + portStart);
        }
        if (portEnd != null && !NetUtils.isValidPort(portEnd)) {
            throw new InvalidParameterValueException("Public port range is an invalid value: " + portEnd);
        }

        // start port can't be bigger than end port
        if (portStart != null && portEnd != null && portStart > portEnd) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }
    }

    @Override
    public boolean revokeNetworkACLItem(long ruleId, boolean apply) {
        Account caller = UserContext.current().getCaller();
        long userId = UserContext.current().getCallerUserId();
        return revokeNetworkACLItem(ruleId, apply, caller, userId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_DELETE, eventDescription = "revoking network acl", async = true)
    protected boolean revokeNetworkACLItem(long ruleId, boolean apply, Account caller, long userId) {

        NetworkACLItemVO rule = _networkACLItemDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find network ACL Item" + ruleId);
        }

       // _accountMgr.checkAccess(caller, null, true, rule);

        revokeRule(rule, caller, userId, false);

        boolean success = false;

        if (apply) {
            try {
                applyNetworkACL(rule.getACLId(), caller);
                success = true;
            } catch (ResourceUnavailableException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            success = true;
        }

        return success;
    }


    @Override
    public Pair<List<? extends NetworkACLItem>, Integer> listNetworkACLItems(ListNetworkACLsCmd cmd) {
        Long networkId = cmd.getNetworkId();
        Long id = cmd.getId();
        String trafficType = cmd.getTrafficType();
        Map<String, String> tags = cmd.getTags();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter filter = new Filter(NetworkACLItemVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<NetworkACLItemVO> sb = _networkACLItemDao.createSearchBuilder();
      //  _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("aclId", sb.entity().getACLId(), Op.EQ);
        sb.and("trafficType", sb.entity().getTrafficType(), Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count=0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<NetworkACLItemVO> sc = sb.create();
        // _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (networkId != null) {
            Network network = _networkDao.findById(networkId);
            sc.setParameters("aclId", network.getNetworkACLId());
        }

        if (trafficType != null) {
            sc.setParameters("trafficType", trafficType);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.NetworkACL.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        Pair<List<NetworkACLItemVO>, Integer> result = _networkACLItemDao.searchAndCount(sc, filter);
        return new Pair<List<? extends NetworkACLItem>, Integer>(result.first(), result.second());
    }

    @Override
    public NetworkACL createNetworkACL(CreateNetworkACLListCmd cmd) {
        NetworkACLVO acl = new NetworkACLVO(cmd.getName(), cmd.getDescription(), cmd.getVpcId());
        _networkACLDao.persist(acl);
        return acl;
    }

    @Override
    public NetworkACL getNetworkACL(long id) {
        return _networkACLDao.findById(id);
    }

    @Override
    public boolean deleteNetworkACL(long id) {
        return _networkACLDao.remove(id);
    }

    @Override
    public Pair<List<? extends NetworkACL>, Integer> listNetworkACLs(ListNetworkACLListsCmd listNetworkACLListsCmd) {
        SearchBuilder<NetworkACLVO> sb = _networkACLDao.createSearchBuilder();
        SearchCriteria<NetworkACLVO> sc = sb.create();
        Filter filter = new Filter(NetworkACLVO.class, "id", false, null, null);
        Pair<List<NetworkACLVO>, Integer> acls =  _networkACLDao.searchAndCount(sc, filter);
        return new Pair<List<? extends NetworkACL>, Integer>(acls.first(), acls.second());
    }

    @Override
    public boolean replaceNetworkACL(long aclId, long networkId) {
        NetworkVO network = _networkDao.findById(networkId);
        if(network == null){
            throw new InvalidParameterValueException("Unable to find Network: " +networkId);
        }
        NetworkACL acl = _networkACLDao.findById(aclId);
        if(acl == null){
            throw new InvalidParameterValueException("Unable to find NetworkACL: " +aclId);
        }
        if(network.getVpcId() == null){
            throw new InvalidParameterValueException("Network does not belong to VPC: " +networkId);
        }
        if(network.getVpcId() != acl.getVpcId()){
            throw new InvalidParameterValueException("Network: "+networkId+" and ACL: "+aclId+" do not belong to the same VPC");
        }
        network.setNetworkACLId(aclId);
        return _networkDao.update(networkId, network);
    }

    @DB
    private void revokeRule(NetworkACLItemVO rule, Account caller, long userId, boolean needUsageEvent) {
        if (caller != null) {
            //_accountMgr.checkAccess(caller, null, true, rule);
        }

        Transaction txn = Transaction.currentTxn();
        boolean generateUsageEvent = false;

        txn.start();
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            _networkACLItemDao.remove(rule.getId());
            generateUsageEvent = true;
        } else if (rule.getState() == State.Add || rule.getState() == State.Active) {
            rule.setState(State.Revoke);
            _networkACLItemDao.update(rule.getId(), rule);
            generateUsageEvent = true;
        }

/*        if (generateUsageEvent && needUsageEvent) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(),
                    null, rule.getClass().getName(), rule.getUuid());
        }*/

        txn.commit();
    }
}
