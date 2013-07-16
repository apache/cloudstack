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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.cloudstack.context.CallContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
@Local(value = { NetworkACLService.class})
public class NetworkACLServiceImpl extends ManagerBase implements NetworkACLService{
    private static final Logger s_logger = Logger.getLogger(NetworkACLServiceImpl.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkACLManager _networkAclMgr;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    VpcManager _vpcMgr;

    @Override
    public NetworkACL createNetworkACL(String name, String description, long vpcId) {
        Account caller = CallContext.current().getCallingAccount();
        Vpc vpc = _vpcMgr.getVpc(vpcId);
        if(vpc == null){
            throw new InvalidParameterValueException("Unable to find VPC");
        }
        _accountMgr.checkAccess(caller, null, true, vpc);
        return _networkAclMgr.createNetworkACL(name, description, vpcId);
    }

    @Override
    public NetworkACL getNetworkACL(long id) {
        return _networkAclMgr.getNetworkACL(id);
    }

    @Override
    public Pair<List<? extends NetworkACL>, Integer> listNetworkACLs(Long id, String name, Long networkId, Long vpcId) {
        SearchBuilder<NetworkACLVO> sb = _networkACLDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("name", sb.entity().getName(), Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), Op.IN);

        if(networkId != null){
            SearchBuilder<NetworkVO> network = _networkDao.createSearchBuilder();
            network.and("networkId", network.entity().getId(), Op.EQ);
            sb.join("networkJoin", network, sb.entity().getId(), network.entity().getNetworkACLId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<NetworkACLVO> sc = sb.create();
        if(id != null){
            sc.setParameters("id", id);
        }

        if(name != null){
            sc.setParameters("name", name);
        }

        if(vpcId != null){
            //Include vpcId 0 to list default ACLs
            sc.setParameters("vpcId", vpcId, 0);
        }

        if(networkId != null){
            sc.setJoinParameters("networkJoin", "networkId", networkId);
        }

        Filter filter = new Filter(NetworkACLVO.class, "id", false, null, null);
        Pair<List<NetworkACLVO>, Integer> acls =  _networkACLDao.searchAndCount(sc, filter);
        return new Pair<List<? extends NetworkACL>, Integer>(acls.first(), acls.second());
    }

    @Override
    public boolean deleteNetworkACL(long id) {
        Account caller = CallContext.current().getCallingAccount();
        NetworkACL acl = _networkACLDao.findById(id);
        if(acl == null) {
            throw new InvalidParameterValueException("Unable to find specified ACL");
        }

        //Do not allow deletion of default ACLs
        if(acl.getId() == NetworkACL.DEFAULT_ALLOW || acl.getId() == NetworkACL.DEFAULT_DENY){
            throw new InvalidParameterValueException("Default ACL cannot be removed");
        }

        Vpc vpc = _vpcMgr.getVpc(acl.getVpcId());
        if(vpc == null){
            throw new InvalidParameterValueException("Unable to find specified VPC associated with the ACL");
        }
        _accountMgr.checkAccess(caller, null, true, vpc);
        return _networkAclMgr.deleteNetworkACL(acl);
    }
    @Override
    public boolean replaceNetworkACLonPrivateGw(long aclId, long privateGatewayId) throws ResourceUnavailableException {
        Account caller = CallContext.current().getCallingAccount();
        VpcGateway gateway = _vpcGatewayDao.findById(privateGatewayId);
        if (gateway == null) {
            throw new InvalidParameterValueException("Unable to find specified private gateway");
        }

        VpcGatewayVO vo = _vpcGatewayDao.findById(privateGatewayId);
        if (vo.getState() != VpcGateway.State.Ready) {
            throw new InvalidParameterValueException("Gateway is not in Ready state");
        }


        NetworkACL acl = _networkACLDao.findById(aclId);
        if(acl == null){
            throw new InvalidParameterValueException("Unable to find specified NetworkACL");
        }

        if (gateway.getVpcId() == null) {
            throw new InvalidParameterValueException("Unable to find specified vpc id");
        }

        if(aclId != NetworkACL.DEFAULT_DENY && aclId != NetworkACL.DEFAULT_ALLOW) {
            Vpc vpc = _vpcMgr.getVpc(acl.getVpcId());
            if(vpc == null){
                throw new InvalidParameterValueException("Unable to find Vpc associated with the NetworkACL");
            }
            _accountMgr.checkAccess(caller, null, true, vpc);
            if(!gateway.getVpcId().equals(acl.getVpcId())){
                throw new InvalidParameterValueException("private gateway: "+privateGatewayId+" and ACL: "+aclId+" do not belong to the same VPC");
            }
        }

        PrivateGateway privateGateway = _vpcMgr.getVpcPrivateGateway(privateGatewayId);
        _accountMgr.checkAccess(caller, null, true, privateGateway);

        return  _networkAclMgr.replaceNetworkACLForPrivateGw(acl, privateGateway);

    }

    @Override
    public boolean replaceNetworkACL(long aclId, long networkId) throws ResourceUnavailableException {
        Account caller = CallContext.current().getCallingAccount();

        NetworkVO network = _networkDao.findById(networkId);
        if(network == null){
            throw new InvalidParameterValueException("Unable to find specified Network");
        }

        NetworkACL acl = _networkACLDao.findById(aclId);
        if(acl == null){
            throw new InvalidParameterValueException("Unable to find specified NetworkACL");
        }

        if(network.getVpcId() == null){
            throw new InvalidParameterValueException("Network is not part of a VPC: "+ network.getUuid());
        }

        if (network.getTrafficType() != Networks.TrafficType.Guest) {
            throw new InvalidParameterValueException("Network ACL can be created just for networks of type " + Networks.TrafficType.Guest);
        }

        if(aclId != NetworkACL.DEFAULT_DENY && aclId != NetworkACL.DEFAULT_ALLOW) {
            //ACL is not default DENY/ALLOW
            // ACL should be associated with a VPC
            Vpc vpc = _vpcMgr.getVpc(acl.getVpcId());
            if(vpc == null){
                throw new InvalidParameterValueException("Unable to find Vpc associated with the NetworkACL");
            }

            _accountMgr.checkAccess(caller, null, true, vpc);
            if(!network.getVpcId().equals(acl.getVpcId())){
                throw new InvalidParameterValueException("Network: "+networkId+" and ACL: "+aclId+" do not belong to the same VPC");
            }
        }

        return _networkAclMgr.replaceNetworkACL(acl, network);
    }

    @Override
    public NetworkACLItem createNetworkACLItem(CreateNetworkACLCmd aclItemCmd){
        Account caller = CallContext.current().getCallingAccount();
        Long aclId = aclItemCmd.getACLId();
        if(aclId == null){
            //ACL id is not specified. Get the ACL details from network
            if(aclItemCmd.getNetworkId() == null){
                throw new InvalidParameterValueException("Cannot create Network ACL Item. ACL Id or network Id is required");
            }
            Network network = _networkMgr.getNetwork(aclItemCmd.getNetworkId());
            if(network.getVpcId() == null){
                throw new InvalidParameterValueException("Network: "+network.getUuid()+" does not belong to VPC");
            }
            aclId = network.getNetworkACLId();

            if(aclId == null){
                //Network is not associated with any ACL. Create a new ACL and add aclItem in it for backward compatibility
                s_logger.debug("Network "+network.getId()+" is not associated with any ACL. Creating an ACL before adding acl item");

                //verify that ACLProvider is supported by network offering
                if(!_networkModel.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Network.Service.NetworkACL)){
                    throw new InvalidParameterValueException("Network Offering does not support NetworkACL service");
                }

                Vpc vpc = _vpcMgr.getVpc(network.getVpcId());
                if(vpc == null){
                    throw new InvalidParameterValueException("Unable to find Vpc associated with the Network");
                }

                //Create new ACL
                String aclName = "VPC_"+vpc.getName()+"_Tier_"+network.getName()+"_ACL_"+network.getUuid();
                String description = "ACL for "+aclName;
                NetworkACL acl = _networkAclMgr.createNetworkACL(aclName, description, network.getVpcId());
                if(acl == null){
                    throw new CloudRuntimeException("Error while create ACL before adding ACL Item for network "+network.getId());
                }
                s_logger.debug("Created ACL: "+aclName+" for network "+network.getId());
                aclId = acl.getId();
                //Apply acl to network
                try {
                    if(!_networkAclMgr.replaceNetworkACL(acl, (NetworkVO)network)){
                        throw new CloudRuntimeException("Unable to apply auto created ACL to network "+network.getId());
                    }
                    s_logger.debug("Created ACL is applied to network "+network.getId());
                } catch (ResourceUnavailableException e) {
                    throw new CloudRuntimeException("Unable to apply auto created ACL to network "+network.getId(), e);
                }
            }
        }

        NetworkACL acl = _networkAclMgr.getNetworkACL(aclId);
        if(acl == null){
            throw new InvalidParameterValueException("Unable to find specified ACL");
        }

        if((aclId == NetworkACL.DEFAULT_DENY) || (aclId == NetworkACL.DEFAULT_ALLOW)){
            throw new InvalidParameterValueException("Default ACL cannot be modified");
        }

        Vpc vpc = _vpcMgr.getVpc(acl.getVpcId());
        if(vpc == null){
            throw new InvalidParameterValueException("Unable to find Vpc associated with the NetworkACL");
        }
        _accountMgr.checkAccess(caller, null, true, vpc);

        //Ensure that number is unique within the ACL
        if(aclItemCmd.getNumber() != null){
            if(_networkACLItemDao.findByAclAndNumber(aclId, aclItemCmd.getNumber()) != null){
                throw new InvalidParameterValueException("ACL item with number "+aclItemCmd.getNumber()+" already exists in ACL: "+acl.getUuid());
            }
        }

        validateNetworkACLItem(aclItemCmd.getSourcePortStart(), aclItemCmd.getSourcePortEnd(), aclItemCmd.getSourceCidrList(),
                aclItemCmd.getProtocol(), aclItemCmd.getIcmpCode(), aclItemCmd.getIcmpType(), aclItemCmd.getAction(), aclItemCmd.getNumber());

        return _networkAclMgr.createNetworkACLItem(aclItemCmd.getSourcePortStart(),
                aclItemCmd.getSourcePortEnd(), aclItemCmd.getProtocol(), aclItemCmd.getSourceCidrList(), aclItemCmd.getIcmpCode(),
                aclItemCmd.getIcmpType(), aclItemCmd.getTrafficType(), aclId, aclItemCmd.getAction(), aclItemCmd.getNumber());
    }

    private void validateNetworkACLItem(Integer portStart, Integer portEnd, List<String> sourceCidrList, String protocol, Integer icmpCode,
                                        Integer icmpType, String action, Integer number) {

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

        // start port and end port must be null for protocol = 'all'
        if ((portStart != null || portEnd != null ) && protocol != null && protocol.equalsIgnoreCase("all"))
            throw new InvalidParameterValueException("start port and end port must be null if protocol = 'all'");

        if (sourceCidrList != null) {
            for (String cidr: sourceCidrList){
                if (!NetUtils.isValidCIDR(cidr)){
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Source cidrs formatting error " + cidr);
                }
            }
        }

        //Validate Protocol
        if(protocol != null){
            //Check if protocol is a number
            if(StringUtils.isNumeric(protocol)){
                int protoNumber = Integer.parseInt(protocol);
                if(protoNumber < 0 || protoNumber > 255){
                    throw new InvalidParameterValueException("Invalid protocol number: " + protoNumber);
                }
            } else {
                //Protocol is not number
                //Check for valid protocol strings
                String supportedProtocols = "tcp,udp,icmp,all";
                if(!supportedProtocols.contains(protocol.toLowerCase())){
                    throw new InvalidParameterValueException("Invalid protocol: " + protocol);
                }
            }

            // icmp code and icmp type can't be passed in for any other protocol rather than icmp
            if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
                throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
            }

            if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
                throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
            }
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

        //Check ofr valid action Allow/Deny
        if(action != null){
            if(!("Allow".equalsIgnoreCase(action) || "Deny".equalsIgnoreCase(action))){
                throw new InvalidParameterValueException("Invalid action. Allowed actions are Allow and Deny");
            }
        }

        //Check for valid number
        if(number != null && number < 1){
            throw new InvalidParameterValueException("Invalid number. Number cannot be < 1");
        }
    }

    @Override
    public NetworkACLItem getNetworkACLItem(long ruleId) {
        return _networkAclMgr.getNetworkACLItem(ruleId);
    }

    @Override
    public boolean applyNetworkACL(long aclId) throws ResourceUnavailableException {
        return _networkAclMgr.applyNetworkACL(aclId);
    }

    @Override
    public Pair<List<? extends NetworkACLItem>, Integer> listNetworkACLItems(ListNetworkACLsCmd cmd) {
        Long networkId = cmd.getNetworkId();
        Long id = cmd.getId();
        Long aclId = cmd.getAclId();
        String trafficType = cmd.getTrafficType();
        String protocol = cmd.getProtocol();
        String action = cmd.getAction();
        Map<String, String> tags = cmd.getTags();

        Account caller = CallContext.current().getCallingAccount();
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
        //_accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("aclId", sb.entity().getAclId(), Op.EQ);
        sb.and("trafficType", sb.entity().getTrafficType(), Op.EQ);
        sb.and("protocol", sb.entity().getProtocol(), Op.EQ);
        sb.and("action", sb.entity().getAction(), Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count=0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), Op.EQ);
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
            aclId = network.getNetworkACLId();
        }

        if (trafficType != null) {
            sc.setParameters("trafficType", trafficType);
        }

        if(aclId != null){
            sc.setParameters("aclId", aclId);
        }

        if(protocol != null){
            sc.setParameters("protocol", protocol);
        }

        if(action != null){
            sc.setParameters("action", action);
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
    public boolean revokeNetworkACLItem(long ruleId) {
        NetworkACLItemVO aclItem = _networkACLItemDao.findById(ruleId);
        if(aclItem != null){
            if((aclItem.getAclId() == NetworkACL.DEFAULT_ALLOW) || (aclItem.getAclId() == NetworkACL.DEFAULT_DENY)){
                throw new InvalidParameterValueException("ACL Items in default ACL cannot be deleted");
            }
        }
        return _networkAclMgr.revokeNetworkACLItem(ruleId);
    }

    @Override
    public NetworkACLItem updateNetworkACLItem(Long id, String protocol, List<String> sourceCidrList, NetworkACLItem.TrafficType trafficType,
                                               String action, Integer number, Integer sourcePortStart, Integer sourcePortEnd, Integer icmpCode,
                                               Integer icmpType) throws ResourceUnavailableException {
        NetworkACLItemVO aclItem = _networkACLItemDao.findById(id);
        if(aclItem == null){
            throw new InvalidParameterValueException("Unable to find ACL Item cannot be found");
        }

        if(aclItem.getAclId() == NetworkACL.DEFAULT_ALLOW || aclItem.getAclId() == NetworkACL.DEFAULT_DENY){
            throw new InvalidParameterValueException("Default ACL Items cannot be updated");
        }

        NetworkACL acl = _networkAclMgr.getNetworkACL(aclItem.getAclId());

        Vpc vpc = _vpcMgr.getVpc(acl.getVpcId());

        Account caller = CallContext.current().getCallingAccount();

        _accountMgr.checkAccess(caller, null, true, vpc);

        if(number != null){
            //Check if ACL Item with specified number already exists
            NetworkACLItemVO aclNumber = _networkACLItemDao.findByAclAndNumber(acl.getId(), number);
            if((aclNumber != null) && (aclNumber.getId() != id)){
                throw new InvalidParameterValueException("ACL item with number "+number+" already exists in ACL: "+acl.getUuid());
            }
        }

        validateNetworkACLItem((sourcePortStart == null) ? aclItem.getSourcePortStart() : sourcePortStart, (sourcePortEnd == null) ? aclItem.getSourcePortEnd() : sourcePortEnd,
                sourceCidrList, protocol, icmpCode, (icmpType == null) ? aclItem.getIcmpType() : icmpType, action, number);

        return _networkAclMgr.updateNetworkACLItem(id, protocol, sourceCidrList, trafficType, action, number, sourcePortStart,
                sourcePortEnd, icmpCode, icmpType);
    }

}