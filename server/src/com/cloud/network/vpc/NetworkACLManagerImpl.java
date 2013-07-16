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

import com.cloud.configuration.ConfigurationManager;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.vpc.NetworkACLItem.State;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.context.CallContext;

import javax.ejb.Local;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;


@Component
@Local(value = { NetworkACLManager.class})
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
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    ConfigurationManager _configMgr;

    @Override
    public NetworkACL createNetworkACL(String name, String description, long vpcId) {
        NetworkACLVO acl = new NetworkACLVO(name, description, vpcId);
        return _networkACLDao.persist(acl);
    }

    @Override
    public boolean applyNetworkACL(long aclId) throws ResourceUnavailableException {
        boolean handled = true;
        boolean aclApplyStatus = true;

        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(aclId);
        //Find all networks using this ACL and apply the ACL
        List<NetworkVO> networks = _networkDao.listByAclId(aclId);
        for(NetworkVO network : networks){
            if(!applyACLItemsToNetwork(network.getId(), rules)) {
                handled = false;
                break;
            }
        }

        List<VpcGatewayVO> vpcGateways = _vpcGatewayDao.listByAclIdAndType(aclId, VpcGateway.Type.Private);
        for (VpcGatewayVO vpcGateway : vpcGateways) {
            PrivateGateway privateGateway = _vpcMgr.getVpcPrivateGateway(vpcGateway.getId());
            if (!applyACLToPrivateGw(privateGateway)) {
                aclApplyStatus = false;
                s_logger.debug("failed to apply network acl item on private gateway " + privateGateway.getId() + "acl id " + aclId);
                break;
            }
        }

        if(handled && aclApplyStatus){
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
        return handled && aclApplyStatus;
    }

    @Override
    public NetworkACL getNetworkACL(long id) {
        return _networkACLDao.findById(id);
    }

    @Override
    public boolean deleteNetworkACL(NetworkACL acl) {
        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(acl.getId());
        if(aclItems.size() > 0){
            throw new CloudRuntimeException("ACL is not empty. Cannot delete network ACL: "+acl.getUuid());
        }

        List<NetworkVO> networks = _networkDao.listByAclId(acl.getId());
        if(networks != null && networks.size() > 0){
            throw new CloudRuntimeException("ACL is still associated with "+networks.size()+" tier(s). Cannot delete network ACL: "+acl.getUuid());
        }

        List<VpcGatewayVO> pvtGateways = _vpcGatewayDao.listByAclIdAndType(acl.getId(), VpcGateway.Type.Private);

        if(pvtGateways != null && pvtGateways.size() > 0){
            throw new CloudRuntimeException("ACL is still associated with "+pvtGateways.size()+" private gateway(s). Cannot delete network ACL: "+acl.getUuid());
        }

        return _networkACLDao.remove(acl.getId());
    }

    @Override
    public boolean replaceNetworkACLForPrivateGw(NetworkACL acl, PrivateGateway gateway) throws ResourceUnavailableException {
        VpcGatewayVO vpcGatewayVo = _vpcGatewayDao.findById(gateway.getId());
        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(acl.getId());
        if (aclItems == null || aclItems.isEmpty()) {
            //Revoke ACL Items of the existing ACL if the new network acl is empty
            //Other wise existing rules will not be removed on the router elelment
            s_logger.debug("New network ACL is empty. Revoke existing rules before applying ACL");
            if(!revokeACLItemsForPrivateGw (gateway)){
                throw new CloudRuntimeException("Failed to replace network ACL. Error while removing existing ACL " +
                        "items for privatewa gateway: "+ gateway.getId());
            }
        }

        vpcGatewayVo.setNetworkACLId(acl.getId());
        if (_vpcGatewayDao.update(vpcGatewayVo.getId(),vpcGatewayVo)) {
            return applyACLToPrivateGw(gateway);

        }
        return false;
    }

    @Override
    public boolean replaceNetworkACL(NetworkACL acl, NetworkVO network) throws ResourceUnavailableException {

        NetworkOffering guestNtwkOff = _configMgr.getNetworkOffering(network.getNetworkOfferingId());

        if (guestNtwkOff == null) {
            throw new InvalidParameterValueException("Can't find network offering associated with network: "+network.getUuid());
        }

        //verify that ACLProvider is supported by network offering
        if(!_ntwkModel.areServicesSupportedByNetworkOffering(guestNtwkOff.getId(), Service.NetworkACL)){
            throw new InvalidParameterValueException("Cannot apply NetworkACL. Network Offering does not support NetworkACL service");
        }

        if(network.getNetworkACLId() != null){
            //Revoke ACL Items of the existing ACL if the new ACL is empty
            //Existing rules won't be removed otherwise
            List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(acl.getId());
            if(aclItems == null || aclItems.isEmpty()){
                s_logger.debug("New network ACL is empty. Revoke existing rules before applying ACL");
               if(!revokeACLItemsForNetwork(network.getId())){
                   throw new CloudRuntimeException("Failed to replace network ACL. Error while removing existing ACL items for network: "+network.getId());
               }
            }
        }

        network.setNetworkACLId(acl.getId());
        //Update Network ACL
        if(_networkDao.update(network.getId(), network)){
            s_logger.debug("Updated network: "+network.getId()+ " with Network ACL Id: "+acl.getId()+", Applying ACL items");
            //Apply ACL to network
            return applyACLToNetwork(network.getId());
        }
        return false;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_ITEM_CREATE, eventDescription = "creating network ACL Item", create = true)
    public NetworkACLItem createNetworkACLItem(Integer portStart, Integer portEnd, String protocol, List<String> sourceCidrList,
                                                  Integer icmpCode, Integer icmpType, NetworkACLItem.TrafficType trafficType, Long aclId,
                                                  String action, Integer number) {
        NetworkACLItem.Action ruleAction = NetworkACLItem.Action.Allow;
        if("deny".equalsIgnoreCase(action)){
            ruleAction = NetworkACLItem.Action.Deny;
        }
        // If number is null, set it to currentMax + 1 (for backward compatibility)
        if(number == null){
            number = _networkACLItemDao.getMaxNumberByACL(aclId) + 1;
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        NetworkACLItemVO newRule = new NetworkACLItemVO(portStart, portEnd, protocol.toLowerCase(), aclId, sourceCidrList, icmpCode, icmpType, trafficType, ruleAction, number);
        newRule = _networkACLItemDao.persist(newRule);

        if (!_networkACLItemDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        CallContext.current().setEventDetails("ACL Item Id: " + newRule.getId());

        txn.commit();

        return getNetworkACLItem(newRule.getId());
    }

    @Override
    public NetworkACLItem getNetworkACLItem(long ruleId) {
        return _networkACLItemDao.findById(ruleId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_DELETE, eventDescription = "revoking network acl", async = true)
    public boolean revokeNetworkACLItem(long ruleId) {

        NetworkACLItemVO rule = _networkACLItemDao.findById(ruleId);

        revokeRule(rule);

        boolean success = false;

        try {
            applyNetworkACL(rule.getAclId());
            success = true;
        } catch (ResourceUnavailableException e) {
            return false;
        }

        return success;
    }

    @DB
    private void revokeRule(NetworkACLItemVO rule) {
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            _networkACLItemDao.remove(rule.getId());
        } else if (rule.getState() == State.Add || rule.getState() == State.Active) {
            rule.setState(State.Revoke);
            _networkACLItemDao.update(rule.getId(), rule);
        }
    }

    @Override
    public boolean revokeACLItemsForNetwork(long networkId) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        if(network.getNetworkACLId() == null){
            return true;
        }
        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(network.getNetworkACLId());
        if (aclItems.isEmpty()) {
            s_logger.debug("Found no network ACL Items for network id=" + networkId);
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + aclItems.size() + " Network ACL Items for network id=" + networkId);
        }

        for (NetworkACLItemVO aclItem : aclItems) {
            // Mark all Network ACLs rules as Revoke, but don't update in DB
            if (aclItem.getState() == State.Add || aclItem.getState() == State.Active) {
                aclItem.setState(State.Revoke);
            }
        }

        boolean success = applyACLItemsToNetwork(network.getId(), aclItems);

        if (s_logger.isDebugEnabled() && success) {
            s_logger.debug("Successfully released Network ACLs for network id=" + networkId + " and # of rules now = "
                    + aclItems.size());
        }

        return success;
    }

    @Override
    public boolean revokeACLItemsForPrivateGw(PrivateGateway gateway) throws ResourceUnavailableException {

        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(gateway.getNetworkACLId());
        if (aclItems.isEmpty()) {
            s_logger.debug("Found no network ACL Items for private gateway  id=" + gateway.getId());
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + aclItems.size() + " Network ACL Items for private gateway  id=" + gateway.getId());
        }

        for (NetworkACLItemVO aclItem : aclItems) {
            // Mark all Network ACLs rules as Revoke, but don't update in DB
            if (aclItem.getState() == State.Add || aclItem.getState() == State.Active) {
                aclItem.setState(State.Revoke);
            }
        }

        boolean success = applyACLToPrivateGw(gateway, aclItems);

        if (s_logger.isDebugEnabled() && success) {
            s_logger.debug("Successfully released Network ACLs for private gateway id=" + gateway.getId() + " and # of rules now = "
                    + aclItems.size());
        }

        return success;
    }

    @Override
    public List<NetworkACLItemVO> listNetworkACLItems(long guestNtwkId) {
        Network network = _networkMgr.getNetwork(guestNtwkId);
        if(network.getNetworkACLId() == null){
            return null;
        }
        return _networkACLItemDao.listByACL(network.getNetworkACLId());
    }

    private void removeRule(NetworkACLItem rule) {
        //remove the rule
        _networkACLItemDao.remove(rule.getId());
    }

    @Override
    public boolean applyACLToPrivateGw(PrivateGateway gateway) throws ResourceUnavailableException {
        VpcGatewayVO vpcGatewayVO = _vpcGatewayDao.findById(gateway.getId());
        List<? extends NetworkACLItem> rules = _networkACLItemDao.listByACL(vpcGatewayVO.getNetworkACLId());
        return applyACLToPrivateGw(gateway, rules);
    }

    private boolean applyACLToPrivateGw(PrivateGateway gateway, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        List<VpcProvider> vpcElements = null;
        vpcElements = new ArrayList<VpcProvider>();
        vpcElements.add((VpcProvider)_ntwkModel.getElementImplementingProvider(Network.Provider.VPCVirtualRouter.getName()));

        if (vpcElements == null) {
            throw new CloudRuntimeException("Failed to initialize vpc elements");
        }

        for (VpcProvider provider: vpcElements){
            return provider.applyACLItemsToPrivateGw(gateway, rules);
            }
        return false;
    }

    @Override
    public boolean applyACLToNetwork(long networkId) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        if(network.getNetworkACLId() == null){
            return true;
        }
        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(network.getNetworkACLId());
        return applyACLItemsToNetwork(networkId, rules);
    }

    @Override
    public NetworkACLItem updateNetworkACLItem(Long id, String protocol, List<String> sourceCidrList, NetworkACLItem.TrafficType trafficType,
                                               String action, Integer number, Integer sourcePortStart, Integer sourcePortEnd, Integer icmpCode,
                                               Integer icmpType) throws ResourceUnavailableException {
        NetworkACLItemVO aclItem = _networkACLItemDao.findById(id);
        aclItem.setState(State.Add);

        if(protocol != null){
            aclItem.setProtocol(protocol);
        }

        if(sourceCidrList != null){
            aclItem.setSourceCidrList(sourceCidrList);
        }

        if(trafficType != null){
            aclItem.setTrafficType(trafficType);
        }

        if(action != null){
            NetworkACLItem.Action ruleAction = NetworkACLItem.Action.Allow;
            if("deny".equalsIgnoreCase(action)){
                ruleAction = NetworkACLItem.Action.Deny;
            }
            aclItem.setAction(ruleAction);
        }

        if(number != null){
            aclItem.setNumber(number);
        }

        if(sourcePortStart != null){
            aclItem.setSourcePortStart(sourcePortStart);
        }

        if(sourcePortEnd != null){
            aclItem.setSourcePortEnd(sourcePortEnd);
        }

        if(icmpCode != null){
            aclItem.setIcmpCode(icmpCode);
        }

        if(icmpType != null){
            aclItem.setIcmpType(icmpType);
        }

        if(_networkACLItemDao.update(id, aclItem)){
            if(applyNetworkACL(aclItem.getAclId())){
                return aclItem;
            } else {
                throw new CloudRuntimeException("Failed to apply Network ACL Item: "+aclItem.getUuid());
            }
        }
        return null;
    }

    public boolean applyACLItemsToNetwork(long networkId, List<NetworkACLItemVO> rules) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        boolean handled = false;
        boolean foundProvider = false;
        for (NetworkACLServiceProvider element: _networkAclElements) {
            Network.Provider provider = element.getProvider();
            boolean  isAclProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.NetworkACL, provider);
            if (!isAclProvider) {
                continue;
            }
            foundProvider = true;
            s_logger.debug("Applying NetworkACL for network: "+network.getId()+" with Network ACL service provider");
            handled = element.applyNetworkACLs(network, rules);
            if (handled)
                break;
        }
        if(!foundProvider){
            s_logger.debug("Unable to find NetworkACL service provider for network: "+network.getId());
        }
        return handled;
    }

}
