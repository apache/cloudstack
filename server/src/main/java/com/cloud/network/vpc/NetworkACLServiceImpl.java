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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLListsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.cloudstack.api.command.user.network.MoveNetworkAclItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLListCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.NetworkACLItem.Action;
import com.cloud.network.vpc.NetworkACLItem.TrafficType;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Component
public class NetworkACLServiceImpl extends ManagerBase implements NetworkACLService {
    private static final Logger s_logger = Logger.getLogger(NetworkACLServiceImpl.class);

    @Inject
    private AccountManager _accountMgr;
    @Inject
    private ResourceTagDao _resourceTagDao;
    @Inject
    private NetworkACLDao _networkACLDao;
    @Inject
    private NetworkACLItemDao _networkACLItemDao;
    @Inject
    private NetworkModel networkModel;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private NetworkACLManager _networkAclMgr;
    @Inject
    private VpcGatewayDao _vpcGatewayDao;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private VpcDao _vpcDao;
    @Inject
    private VpcService _vpcSvc;

    private String supportedProtocolsForAclRules = "tcp,udp,icmp,all";

    @Override
    public NetworkACL createNetworkACL(final String name, final String description, final long vpcId, final Boolean forDisplay) {
        final Account caller = CallContext.current().getCallingAccount();
        final Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Unable to find VPC");
        }
        _accountMgr.checkAccess(caller, null, true, vpc);
        return _networkAclMgr.createNetworkACL(name, description, vpcId, forDisplay);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_CREATE, eventDescription = "creating network acl list", async = true)
    public NetworkACL getNetworkACL(final long id) {
        return _networkAclMgr.getNetworkACL(id);
    }

    @Override
    public Pair<List<? extends NetworkACL>, Integer> listNetworkACLs(final ListNetworkACLListsCmd cmd) {
        final Long id = cmd.getId();
        final String name = cmd.getName();
        final Long networkId = cmd.getNetworkId();
        final Long vpcId = cmd.getVpcId();
        final String keyword = cmd.getKeyword();
        final Boolean display = cmd.getDisplay();

        final SearchBuilder<NetworkACLVO> sb = _networkACLDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("name", sb.entity().getName(), Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), Op.IN);
        sb.and("display", sb.entity().isDisplay(), Op.EQ);

        final Account caller = CallContext.current().getCallingAccount();

        if (networkId != null) {
            final SearchBuilder<NetworkVO> network = _networkDao.createSearchBuilder();
            network.and("networkId", network.entity().getId(), Op.EQ);
            sb.join("networkJoin", network, sb.entity().getId(), network.entity().getNetworkACLId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<NetworkACLVO> sc = sb.create();

        if (keyword != null) {
            final SearchCriteria<NetworkACLVO> ssc = _networkACLDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (vpcId != null) {
            final Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);
            if (vpc == null) {
                throw new InvalidParameterValueException("Unable to find VPC");
            }
            _accountMgr.checkAccess(caller, null, true, vpc);
            //Include vpcId 0 to list default ACLs
            sc.setParameters("vpcId", vpcId, 0);
        } else {
            //ToDo: Add accountId to network_acl table for permission check

            // VpcId is not specified. Find permitted VPCs for the caller
            // and list ACLs belonging to the permitted VPCs
            final List<Long> permittedAccounts = new ArrayList<Long>();
            Long domainId = cmd.getDomainId();
            boolean isRecursive = cmd.isRecursive();
            final String accountName = cmd.getAccountName();
            final Long projectId = cmd.getProjectId();
            final boolean listAll = cmd.listAll();
            final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive, null);
            _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
            domainId = domainIdRecursiveListProject.first();
            isRecursive = domainIdRecursiveListProject.second();
            final ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
            final SearchBuilder<VpcVO> sbVpc = _vpcDao.createSearchBuilder();
            _accountMgr.buildACLSearchBuilder(sbVpc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            final SearchCriteria<VpcVO> scVpc = sbVpc.create();
            _accountMgr.buildACLSearchCriteria(scVpc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            final List<VpcVO> vpcs = _vpcDao.search(scVpc, null);
            final List<Long> vpcIds = new ArrayList<Long>();
            for (final VpcVO vpc : vpcs) {
                vpcIds.add(vpc.getId());
            }
            //Add vpc_id 0 to list default ACLs
            vpcIds.add(0L);
            sc.setParameters("vpcId", vpcIds.toArray());
        }

        if (networkId != null) {
            sc.setJoinParameters("networkJoin", "networkId", networkId);
        }

        final Filter filter = new Filter(NetworkACLVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        final Pair<List<NetworkACLVO>, Integer> acls = _networkACLDao.searchAndCount(sc, filter);
        return new Pair<List<? extends NetworkACL>, Integer>(acls.first(), acls.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_DELETE, eventDescription = "Deleting Network ACL List", async = true)
    public boolean deleteNetworkACL(final long id) {
        final Account caller = CallContext.current().getCallingAccount();
        final NetworkACL acl = _networkACLDao.findById(id);
        if (acl == null) {
            throw new InvalidParameterValueException("Unable to find specified ACL");
        }

        //Do not allow deletion of default ACLs
        if (acl.getId() == NetworkACL.DEFAULT_ALLOW || acl.getId() == NetworkACL.DEFAULT_DENY) {
            throw new InvalidParameterValueException("Default ACL cannot be removed");
        }

        final Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Unable to find specified VPC associated with the ACL");
        }
        _accountMgr.checkAccess(caller, null, true, vpc);
        return _networkAclMgr.deleteNetworkACL(acl);
    }

    @Override
    public boolean replaceNetworkACLonPrivateGw(final long aclId, final long privateGatewayId) throws ResourceUnavailableException {
        final Account caller = CallContext.current().getCallingAccount();
        final VpcGateway gateway = _vpcGatewayDao.findById(privateGatewayId);
        if (gateway == null) {
            throw new InvalidParameterValueException("Unable to find specified private gateway");
        }

        final VpcGatewayVO vo = _vpcGatewayDao.findById(privateGatewayId);
        if (vo.getState() != VpcGateway.State.Ready) {
            throw new InvalidParameterValueException("Gateway is not in Ready state");
        }

        final NetworkACL acl = _networkACLDao.findById(aclId);
        if (acl == null) {
            throw new InvalidParameterValueException("Unable to find specified NetworkACL");
        }

        if (gateway.getVpcId() == null) {
            throw new InvalidParameterValueException("Unable to find specified vpc id");
        }

        if (aclId != NetworkACL.DEFAULT_DENY && aclId != NetworkACL.DEFAULT_ALLOW) {
            final Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());
            if (vpc == null) {
                throw new InvalidParameterValueException("Unable to find Vpc associated with the NetworkACL");
            }
            _accountMgr.checkAccess(caller, null, true, vpc);
            if (!gateway.getVpcId().equals(acl.getVpcId())) {
                throw new InvalidParameterValueException("private gateway: " + privateGatewayId + " and ACL: " + aclId + " do not belong to the same VPC");
            }
        }

        final PrivateGateway privateGateway = _vpcSvc.getVpcPrivateGateway(gateway.getId());
        _accountMgr.checkAccess(caller, null, true, privateGateway);

        return _networkAclMgr.replaceNetworkACLForPrivateGw(acl, privateGateway);

    }

    @Override
    public boolean replaceNetworkACL(final long aclId, final long networkId) throws ResourceUnavailableException {
        final Account caller = CallContext.current().getCallingAccount();

        final NetworkVO network = _networkDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Unable to find specified Network");
        }

        final NetworkACL acl = _networkACLDao.findById(aclId);
        if (acl == null) {
            throw new InvalidParameterValueException("Unable to find specified NetworkACL");
        }

        if (network.getVpcId() == null) {
            throw new InvalidParameterValueException("Network is not part of a VPC: " + network.getUuid());
        }

        if (network.getTrafficType() != Networks.TrafficType.Guest) {
            throw new InvalidParameterValueException("Network ACL can be created just for networks of type " + Networks.TrafficType.Guest);
        }

        if (aclId != NetworkACL.DEFAULT_DENY && aclId != NetworkACL.DEFAULT_ALLOW) {
            //ACL is not default DENY/ALLOW
            // ACL should be associated with a VPC
            final Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());
            if (vpc == null) {
                throw new InvalidParameterValueException("Unable to find Vpc associated with the NetworkACL");
            }

            _accountMgr.checkAccess(caller, null, true, vpc);
            if (!network.getVpcId().equals(acl.getVpcId())) {
                throw new InvalidParameterValueException("Network: " + networkId + " and ACL: " + aclId + " do not belong to the same VPC");
            }
        }

        return _networkAclMgr.replaceNetworkACL(acl, network);
    }

    /**
     * Creates and persists a network ACL rule. The network ACL rule is persisted as a {@link NetworkACLItemVO}.
     * If no ACL list ID is informed, we will create one. To check these details, please refer to {@link #createAclListIfNeeded(CreateNetworkACLCmd)}.
     * All of the attributes will be validated accordingly using the following methods:
     * <ul>
     * <li> {@link #validateAclRuleNumber(CreateNetworkACLCmd, Long, NetworkACL)} to validate the provided ACL rule number;
     * <li> {@link #validateNetworkAclList(Long, NetworkACL)} to check if the user has access to the informed ACL list ID and respective VPC;
     * <li> {@link #validateAndCreateNetworkAclRuleAction(String)} to validate the ACL rule action;
     * <li> {@link #validateNetworkACLItem(NetworkACLItemVO)} to validate general configurations relating to protocol, ports, and ICMP codes and types.
     * </ul>
     *
     * Moreover, if not ACL rule number is provided we generate one based on the last ACL number used. We will increment +1 in the last ACL rule number used. After all of the validation the ACL rule is persisted using the method {@link NetworkACLManagerImpl#createNetworkACLItem(NetworkACLItemVO)}.
     */
    @Override
    public NetworkACLItem createNetworkACLItem(CreateNetworkACLCmd createNetworkACLCmd) {
        Long aclId = createAclListIfNeeded(createNetworkACLCmd);

        Integer sourcePortStart = createNetworkACLCmd.getSourcePortStart();
        Integer sourcePortEnd = createNetworkACLCmd.getSourcePortEnd();
        String protocol = createNetworkACLCmd.getProtocol();
        List<String> sourceCidrList = createNetworkACLCmd.getSourceCidrList();
        Integer icmpCode = createNetworkACLCmd.getIcmpCode();
        Integer icmpType = createNetworkACLCmd.getIcmpType();
        TrafficType trafficType = createNetworkACLCmd.getTrafficType();
        String reason = createNetworkACLCmd.getReason();
        String action = createNetworkACLCmd.getAction();

        NetworkACL acl = _networkAclMgr.getNetworkACL(aclId);

        validateNetworkAcl(acl);
        validateAclRuleNumber(createNetworkACLCmd, acl);

        NetworkACLItem.Action ruleAction = validateAndCreateNetworkAclRuleAction(action);
        Integer number = createNetworkACLCmd.getNumber();
        if (number == null) {
            number = _networkACLItemDao.getMaxNumberByACL(aclId) + 1;
        }
        NetworkACLItemVO networkACLItemVO = new NetworkACLItemVO(sourcePortStart, sourcePortEnd, protocol, aclId, sourceCidrList, icmpCode, icmpType, trafficType, ruleAction, number, reason);
        networkACLItemVO.setDisplay(createNetworkACLCmd.isDisplay());

        validateNetworkACLItem(networkACLItemVO);
        return _networkAclMgr.createNetworkACLItem(networkACLItemVO);
    }

    /**
     *  We first validate the given ACL action as a string using {@link #validateNetworkAclRuleAction(String)}.
     *  Afterwards, we convert this ACL to an object of {@link NetworkACLItem.Action}.
     *  If the action as String matches the word 'deny' (ignoring case), we return an instance of {@link NetworkACLItem.Action#Deny}.
     *  Otherwise, we return {@link NetworkACLItem.Action#Allow}.
     */
    protected NetworkACLItem.Action validateAndCreateNetworkAclRuleAction(String action) {
        validateNetworkAclRuleAction(action);
        NetworkACLItem.Action ruleAction = NetworkACLItem.Action.Allow;
        if ("deny".equalsIgnoreCase(action)) {
            ruleAction = NetworkACLItem.Action.Deny;
        }
        return ruleAction;
    }

    /**
     * Validates the network ACL rule action given as a {@link String}.
     * If the parameter is null, we do not perform any validations. Otherwise, we check if the parameter is equal to 'Allow' or 'Deny' (ignoring the case).
     * If the parameter is an invalid action, we throw an {@link InvalidParameterValueException}.
     */
    protected void validateNetworkAclRuleAction(String action) {
        if (action != null) {
            if (!("Allow".equalsIgnoreCase(action) || "Deny".equalsIgnoreCase(action))) {
                throw new InvalidParameterValueException(String.format("Invalid action [%s]. Permitted actions are Allow and Deny", action));
            }
        }
    }

    /**
     * Validates the ACL rule number field. If the field is null, then we do not have anything to check here.
     * If the number is not null, we perform the following checks:
     * <ul>
     *  <li>If number is less than one, than we throw an {@link InvalidParameterValueException};
     *  <li>if there is already an ACL configured with the given number for the network, we also throw an {@link InvalidParameterValueException}. The check is performed using {@link NetworkACLItemDao#findByAclAndNumber(long, int)} method.
     * </ul>
     *
     * At the end, if not exception is thrown, the number of the ACL rule is valid.
     */
    protected void validateAclRuleNumber(CreateNetworkACLCmd createNetworkAclCmd, NetworkACL acl) {
        Integer number = createNetworkAclCmd.getNumber();
        if (number != null) {
            if (number < 1) {
                throw new InvalidParameterValueException(String.format("Invalid number [%d]. Number cannot be < 1", number));
            }
            if (_networkACLItemDao.findByAclAndNumber(acl.getId(), createNetworkAclCmd.getNumber()) != null) {
                throw new InvalidParameterValueException("ACL item with number " + number + " already exists in ACL: " + acl.getUuid());
            }
        }
    }

    /**
     * Validates a given {@link NetworkACL}. The validations are the following:
     * <ul>
     *  <li>If the parameter is null, we return an  {@link InvalidParameterValueException};
     *  <li>Default ACLs {@link NetworkACL#DEFAULT_ALLOW} and {@link NetworkACL#DEFAULT_DENY} cannot be modified. Therefore, if any of them is provided we throw a {@link InvalidParameterValueException};
     *  <li>If the network does not have a VPC, we will throw an {@link InvalidParameterValueException}.
     * </ul>
     *
     * After all validations, we check if the user has access to the given network ACL using {@link AccountManager#checkAccess(Account, org.apache.cloudstack.acl.SecurityChecker.AccessType, boolean, org.apache.cloudstack.acl.ControlledEntity...)}.
     */
    protected void validateNetworkAcl(NetworkACL acl) {
        if (acl == null) {
            throw new InvalidParameterValueException("Unable to find specified ACL.");
        }

        if (acl.getId() == NetworkACL.DEFAULT_DENY || acl.getId() == NetworkACL.DEFAULT_ALLOW) {
            throw new InvalidParameterValueException("Default ACL cannot be modified");
        }

        Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException(String.format("Unable to find Vpc associated with the NetworkACL [%s]", acl.getUuid()));
        }
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, vpc);
    }

    /**
     * This methods will simply return the ACL rule list ID if it has been provided by the parameter 'createNetworkACLCmd'.
     * If no ACL rule List ID has been provided the method behave as follows:
     * <ul>
     *  <li> If it has not been provided either, we will throw an {@link InvalidParameterValueException};
     *  <li> if the network ID has been provided, we will check if the network has a VPC; if it does not have, we will throw an {@link InvalidParameterValueException};
     *  <ul>
     *      <li> If the VPC already has an ACL rule list, we will return it;
     *      <li> otherwise, we will create one using {@link #createAclListForNetworkAndReturnAclListId(CreateNetworkACLCmd, Network)} method. This behavior is a legacy thing that has been maintained so far.
     *  </ul>
     * </ul>
     *
     * @return The network ACL list ID
     */
    protected Long createAclListIfNeeded(CreateNetworkACLCmd createNetworkACLCmd) {
        Long aclId = createNetworkACLCmd.getACLId();
        if (aclId != null) {
            return aclId;
        }
        if (createNetworkACLCmd.getNetworkId() == null) {
            throw new InvalidParameterValueException("Cannot create Network ACL Item. ACL Id or network Id is required");
        }
        Network network = networkModel.getNetwork(createNetworkACLCmd.getNetworkId());
        if (network.getVpcId() == null) {
            throw new InvalidParameterValueException("Network: " + network.getUuid() + " does not belong to VPC");
        }
        aclId = network.getNetworkACLId();

        if (aclId == null) {
            aclId = createAclListForNetworkAndReturnAclListId(createNetworkACLCmd, network);
        }
        return aclId;
    }

    /**
     * This method will created a network ACL for the provided network. This method will behave as follows:
     * <ul>
     *  <li> If the network offering does not support ACLs ( {@link NetworkModel#areServicesSupportedByNetworkOffering(long, com.cloud.network.Network.Service...)} ), then it throws an {@link InvalidParameterValueException};
     *  <li> If the network does not have any VPC, it throws an {@link InvalidParameterValueException};
     *  <li> If everything is OK so far, we try to create the ACL using {@link NetworkACLManagerImpl#createNetworkACL(String, String, long, Boolean)} method.
     *  <ul>
     *      <li> If the ACL is not created we throw a {@link CloudRuntimeException};
     *      <li> otherwise, the workflow continues.
     *  </ul>
     *  <li> With the ACL in our hands, we try to apply it. If it does not work we throw a {@link CloudRuntimeException}.
     * </ul>
     *
     * @return the Id of the network ACL that is created.
     */
    protected Long createAclListForNetworkAndReturnAclListId(CreateNetworkACLCmd aclItemCmd, Network network) {
        s_logger.debug("Network " + network.getId() + " is not associated with any ACL. Creating an ACL before adding acl item");

        if (!networkModel.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Network.Service.NetworkACL)) {
            throw new InvalidParameterValueException("Network Offering does not support NetworkACL service");
        }

        Vpc vpc = _entityMgr.findById(Vpc.class, network.getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Unable to find Vpc associated with the Network");
        }

        String aclName = "VPC_" + vpc.getName() + "_Tier_" + network.getName() + "_ACL_" + network.getUuid();
        String description = "ACL for " + aclName;
        NetworkACL acl = _networkAclMgr.createNetworkACL(aclName, description, network.getVpcId(), aclItemCmd.isDisplay());
        if (acl == null) {
            throw new CloudRuntimeException("Error while create ACL before adding ACL Item for network " + network.getId());
        }
        s_logger.debug("Created ACL: " + aclName + " for network " + network.getId());
        Long aclId = acl.getId();
        //Apply acl to network
        try {
            if (!_networkAclMgr.replaceNetworkACL(acl, (NetworkVO)network)) {
                throw new CloudRuntimeException("Unable to apply auto created ACL to network " + network.getId());
            }
            s_logger.debug("Created ACL is applied to network " + network.getId());
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to apply auto created ACL to network " + network.getId(), e);
        }
        return aclId;
    }

    /**
     *  Performs all of the validations for the {@link NetworkACLItem}.
     *  First we validate the sources start and end ports using {@link #validateSourceStartAndEndPorts(NetworkACLItemVO)};
     *  then, we validate the source CIDR list using {@link #validateSourceCidrList(NetworkACLItemVO)};
     *  afterwards, it is validated the protocol entered in the {@link NetworkACLItemVO} using {@link #validateProtocol(NetworkACLItemVO)}.
     */
    protected void validateNetworkACLItem(NetworkACLItemVO networkACLItemVO) {
        validateSourceStartAndEndPorts(networkACLItemVO);
        validateSourceCidrList(networkACLItemVO);
        validateProtocol(networkACLItemVO);
    }

    /**
     * Validated ICMP type and code of {@link NetworkACLItemVO}. The behavior of this method is the following:
     * <ul>
     *  <li>If no ICMP type is provided, we do not perform validations;
     *  <li>If the ICMP type is not '-1', we validate it using {@link NetUtils#validateIcmpType(long)};
     *  <li>If the ICMP code is null, we do not perform validations;
     *  <li>If the ICMP code is not '-1', we validate it using {@link NetUtils#validateIcmpCode(long)};
     * </ul>
     * Failing to meet the above conditions, we throw an {@link InvalidParameterValueException}.
     */
    protected void validateIcmpTypeAndCode(NetworkACLItemVO networkACLItemVO) {
        Integer icmpType = networkACLItemVO.getIcmpType();
        Integer icmpCode = networkACLItemVO.getIcmpCode();
        if (icmpType == null) {
            return;
        }
        if (icmpType.longValue() != -1 && !NetUtils.validateIcmpType(icmpType.longValue())) {
            throw new InvalidParameterValueException(String.format("Invalid icmp type [%d]. It should belong to [0-255] range", icmpType));
        }
        if (icmpCode != null) {
            if (icmpCode.longValue() != -1 && !NetUtils.validateIcmpCode(icmpCode.longValue())) {
                throw new InvalidParameterValueException(String.format("Invalid icmp code [%d]. It should belong to [0-16] range and can be defined when icmpType belongs to [0-40] range", icmpCode));
            }
        }
    }

    /**
     *   Validates the {@link NetworkACLItemVO} protocol. If the protocol is blank, we do not execute any validations. Otherwise, we perform the following checks:
     *   <ul>
     *      <li>If it is a numeric value, the protocol must be bigger or equal to 0 and smaller or equal to 255;
     *      <li>if it is a {@link String}, it must be one of the following: {@link #supportedProtocolsForAclRules};
     *   </ul>
     *    Whenever the conditions enumerated above are not met, we throw an {@link InvalidParameterValueException}.
     *
     *    If the parameter passes the protocol type validations, we check the following:
     *    <ul>
     *      <li>If it is not an ICMP type protocol, it cannot have any value in {@link NetworkACLItemVO#getIcmpCode()} and {@link NetworkACLItemVO#getIcmpType()};
     *      <li>If it is an ICMP type protocol, it cannot have any value in {@link NetworkACLItemVO#getSourcePortStart()} and {@link NetworkACLItemVO#getSourcePortEnd()}.
     *    </ul>
     *    Failing to meet the above conditions, we throw an {@link InvalidParameterValueException}.
     *
     *    The last check is performed via {@link #validateIcmpTypeAndCode(NetworkACLItemVO)} method.
     */
    protected void validateProtocol(NetworkACLItemVO networkACLItemVO) {
        String protocol = networkACLItemVO.getProtocol();
        if (StringUtils.isBlank(protocol)) {
            return;
        }
        if (StringUtils.isNumeric(protocol)) {
            int protoNumber = Integer.parseInt(protocol);
            if (protoNumber < 0 || protoNumber > 255) {
                throw new InvalidParameterValueException("Invalid protocol number: " + protoNumber);
            }
        } else {
            if (!supportedProtocolsForAclRules.contains(protocol.toLowerCase())) {
                throw new InvalidParameterValueException(String.format("Invalid protocol [%s]. Expected one of: [%s]", protocol, supportedProtocolsForAclRules));
            }
        }

        Integer icmpCode = networkACLItemVO.getIcmpCode();
        Integer icmpType = networkACLItemVO.getIcmpType();
        // icmp code and icmp type can't be passed in for any other protocol rather than icmp
        boolean isIcmpProtocol = protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO);
        if (!isIcmpProtocol && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }

        Integer sourcePortStart = networkACLItemVO.getSourcePortStart();
        Integer sourcePortEnd = networkACLItemVO.getSourcePortEnd();
        if (isIcmpProtocol && (sourcePortStart != null || sourcePortEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        }

        validateIcmpTypeAndCode(networkACLItemVO);
    }

    /**
     *  Validates all of the CIDRs in the {@link NetworkACLItemVO#getSourceCidrList()}.
     *  If the list is empty we do not execute any validation. Otherwise, all of the CIDRs are validated using {@link NetUtils#isValidIp4Cidr(String)}.
     */
    protected void validateSourceCidrList(NetworkACLItemVO networkACLItemVO) {
        List<String> sourceCidrList = networkACLItemVO.getSourceCidrList();
        if (CollectionUtils.isNotEmpty(sourceCidrList)) {
            for (String cidr : sourceCidrList) {
                if (!NetUtils.isValidIp4Cidr(cidr) && !NetUtils.isValidIp6Cidr(cidr)) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Source cidrs formatting error " + cidr);
                }
            }
        }
    }

    /**
     * Validates the source start and end ports for the given network ACL rule.
     * If both ports (start and end) are null, we do not execute validations. Otherwise, we check the following:
     * <ul>
     *  <li> Check if start port is valid using {@link NetUtils#isValidPort(int)};
     *  <li> Check if end port is valid using {@link NetUtils#isValidPort(int)};
     *  <li> Check if start port is bigger than end port;
     *  <li> Check if start and end ports were used with protocol 'all'
     *  </ul>
     *  All of the above cases will generate an {@link InvalidParameterValueException}.
     */
    protected void validateSourceStartAndEndPorts(NetworkACLItemVO networkACLItemVO) {
        Integer sourcePortStart = networkACLItemVO.getSourcePortStart();
        Integer sourcePortEnd = networkACLItemVO.getSourcePortEnd();
        if (sourcePortStart == null && sourcePortEnd == null) {
            return;
        }

        if (!NetUtils.isValidPort(sourcePortStart)) {
            throw new InvalidParameterValueException("Start public port is an invalid value: " + sourcePortStart);
        }

        if (!NetUtils.isValidPort(sourcePortEnd)) {
            throw new InvalidParameterValueException("End public port is an invalid value: " + sourcePortEnd);
        }
        if (sourcePortStart > sourcePortEnd) {
            throw new InvalidParameterValueException(String.format("Start port can't be bigger than end port [startport=%d,endport=%d]", sourcePortStart, sourcePortEnd));
        }
        String protocol = networkACLItemVO.getProtocol();
        if ("all".equalsIgnoreCase(protocol)) {
            throw new InvalidParameterValueException("start port and end port must be null if protocol = 'all'");
        }
    }

    @Override
    public NetworkACLItem getNetworkACLItem(final long ruleId) {
        return _networkAclMgr.getNetworkACLItem(ruleId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_ITEM_CREATE, eventDescription = "Applying Network ACL Item", async = true)
    public boolean applyNetworkACL(final long aclId) throws ResourceUnavailableException {
        return _networkAclMgr.applyNetworkACL(aclId);
    }

    @Override
    public Pair<List<? extends NetworkACLItem>, Integer> listNetworkACLItems(final ListNetworkACLsCmd cmd) {
        final Long networkId = cmd.getNetworkId();
        final Long id = cmd.getId();
        Long aclId = cmd.getAclId();
        final String trafficType = cmd.getTrafficType();
        final String protocol = cmd.getProtocol();
        final String action = cmd.getAction();
        final Map<String, String> tags = cmd.getTags();
        final Account caller = CallContext.current().getCallingAccount();

        final Filter filter = new Filter(NetworkACLItemVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchBuilder<NetworkACLItemVO> sb = _networkACLItemDao.createSearchBuilder();

        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("aclId", sb.entity().getAclId(), Op.EQ);
        sb.and("trafficType", sb.entity().getTrafficType(), Op.EQ);
        sb.and("protocol", sb.entity().getProtocol(), Op.EQ);
        sb.and("action", sb.entity().getAction(), Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            final SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        if (aclId == null) {
            //Join with network_acl table when aclId is not specified to list acl_items within permitted VPCs
            final SearchBuilder<NetworkACLVO> vpcSearch = _networkACLDao.createSearchBuilder();
            vpcSearch.and("vpcId", vpcSearch.entity().getVpcId(), Op.IN);
            sb.join("vpcSearch", vpcSearch, sb.entity().getAclId(), vpcSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        final SearchCriteria<NetworkACLItemVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (networkId != null) {
            final Network network = _networkDao.findById(networkId);
            aclId = network.getNetworkACLId();
            if (aclId == null) {
                // No aclId associated with the network.
                //Return empty list
                return new Pair(new ArrayList<NetworkACLItem>(), 0);
            }
        }

        if (trafficType != null) {
            sc.setParameters("trafficType", trafficType);
        }

        if (aclId != null) {
            // Get VPC and check access
            final NetworkACL acl = _networkACLDao.findById(aclId);
            if (acl.getVpcId() != 0) {
                final Vpc vpc = _vpcDao.findById(acl.getVpcId());
                if (vpc == null) {
                    throw new InvalidParameterValueException("Unable to find VPC associated with acl");
                }
                _accountMgr.checkAccess(caller, null, true, vpc);
            }
            sc.setParameters("aclId", aclId);
        } else {
            //ToDo: Add accountId to network_acl_item table for permission check

            // aclId is not specified
            // List permitted VPCs and filter aclItems
            final List<Long> permittedAccounts = new ArrayList<Long>();
            Long domainId = cmd.getDomainId();
            boolean isRecursive = cmd.isRecursive();
            final String accountName = cmd.getAccountName();
            final Long projectId = cmd.getProjectId();
            final boolean listAll = cmd.listAll();
            final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive, null);
            _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
            domainId = domainIdRecursiveListProject.first();
            isRecursive = domainIdRecursiveListProject.second();
            final ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
            final SearchBuilder<VpcVO> sbVpc = _vpcDao.createSearchBuilder();
            _accountMgr.buildACLSearchBuilder(sbVpc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            final SearchCriteria<VpcVO> scVpc = sbVpc.create();
            _accountMgr.buildACLSearchCriteria(scVpc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            final List<VpcVO> vpcs = _vpcDao.search(scVpc, null);
            final List<Long> vpcIds = new ArrayList<Long>();
            for (final VpcVO vpc : vpcs) {
                vpcIds.add(vpc.getId());
            }
            //Add vpc_id 0 to list acl_items in default ACL
            vpcIds.add(0L);
            sc.setJoinParameters("vpcSearch", "vpcId", vpcIds.toArray());
        }

        if (protocol != null) {
            sc.setParameters("protocol", protocol);
        }

        if (action != null) {
            sc.setParameters("action", action);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.NetworkACL.toString());
            for (final String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        final Pair<List<NetworkACLItemVO>, Integer> result = _networkACLItemDao.searchAndCount(sc, filter);
        final List<NetworkACLItemVO> aclItemVOs = result.first();
        for (final NetworkACLItemVO item : aclItemVOs) {
            _networkACLItemDao.loadCidrs(item);
        }
        return new Pair<List<? extends NetworkACLItem>, Integer>(aclItemVOs, result.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_ITEM_DELETE, eventDescription = "Deleting Network ACL Item", async = true)
    public boolean revokeNetworkACLItem(final long ruleId) {
        final NetworkACLItemVO aclItem = _networkACLItemDao.findById(ruleId);
        if (aclItem != null) {
            final NetworkACL acl = _networkAclMgr.getNetworkACL(aclItem.getAclId());

            final Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());

            if (aclItem.getAclId() == NetworkACL.DEFAULT_ALLOW || aclItem.getAclId() == NetworkACL.DEFAULT_DENY) {
                throw new InvalidParameterValueException("ACL Items in default ACL cannot be deleted");
            }

            final Account caller = CallContext.current().getCallingAccount();

            _accountMgr.checkAccess(caller, null, true, vpc);

        }
        return _networkAclMgr.revokeNetworkACLItem(ruleId);
    }

    /**
     * Updates a network ACL with the given values found in the {@link UpdateNetworkACLItemCmd} parameter.
     * First we will validate the network ACL rule provided in the command using {@link #validateNetworkAclRuleIdAndRetrieveIt(UpdateNetworkACLItemCmd)}.
     * Then, we validate the ACL itself using {@link #validateNetworkAcl(NetworkACL)}. If all of the validation is ok, we do the following.
     * <ul>
     *  <li>Transfer new data to {@link NetworkACLItemVO} that is intended to be updated;
     *  <li>Validate the ACL rule being updated using {@link #validateNetworkACLItem(NetworkACLItemVO)}.
     * </ul>
     *
     * After the validations and updating the POJO we execute the update in the database using {@link NetworkACLManagerImpl#updateNetworkACLItem(NetworkACLItemVO)}.
     *
     */
    @Override
    public NetworkACLItem updateNetworkACLItem(UpdateNetworkACLItemCmd updateNetworkACLItemCmd) throws ResourceUnavailableException {
        NetworkACLItemVO networkACLItemVo = validateNetworkAclRuleIdAndRetrieveIt(updateNetworkACLItemCmd);

        NetworkACL acl = _networkAclMgr.getNetworkACL(networkACLItemVo.getAclId());
        validateNetworkAcl(acl);

        transferDataToNetworkAclRulePojo(updateNetworkACLItemCmd, networkACLItemVo, acl);
        validateNetworkACLItem(networkACLItemVo);
        return _networkAclMgr.updateNetworkACLItem(networkACLItemVo);
    }

    /**
     *  We transfer the update information form {@link UpdateNetworkACLItemCmd} to the {@link NetworkACLItemVO} POJO passed as parameter.
     *  There is one validation performed here, which is regarding the number of the ACL. We will check if there is already an ACL rule with that number, and if this is the case an {@link InvalidParameterValueException} is thrown.
     *  All of the parameters in {@link UpdateNetworkACLItemCmd} that are not null will be set to their corresponding fields in {@link NetworkACLItemVO}.
     *  If the parameter {@link UpdateNetworkACLItemCmd#isPartialUpgrade()} returns false, we will use null parameters, which will allow us to completely update the ACL rule.
     *  However, the number and custom Uuid will never be set to null. Therefore, if it is not a partial upgrade, these values will remain the same.
     *
     *  We use {@link #validateAndCreateNetworkAclRuleAction(String)} when converting an action as {@link String} to its Enum corresponding value.
     */
    protected void transferDataToNetworkAclRulePojo(UpdateNetworkACLItemCmd updateNetworkACLItemCmd, NetworkACLItemVO networkACLItemVo, NetworkACL acl) {
        Integer number = updateNetworkACLItemCmd.getNumber();
        if (number != null) {
            NetworkACLItemVO aclNumber = _networkACLItemDao.findByAclAndNumber(acl.getId(), number);
            if (aclNumber != null && aclNumber.getId() != networkACLItemVo.getId()) {
                throw new InvalidParameterValueException("ACL item with number " + number + " already exists in ACL: " + acl.getUuid());
            }
            networkACLItemVo.setNumber(number);
        }
        boolean isPartialUpgrade = updateNetworkACLItemCmd.isPartialUpgrade();

        Integer sourcePortStart = updateNetworkACLItemCmd.getSourcePortStart();
        if (!isPartialUpgrade || sourcePortStart != null) {
            networkACLItemVo.setSourcePortStart(sourcePortStart);
        }
        Integer sourcePortEnd = updateNetworkACLItemCmd.getSourcePortEnd();
        if (!isPartialUpgrade || sourcePortEnd != null) {
            networkACLItemVo.setSourcePortEnd(sourcePortEnd);
        }
        List<String> sourceCidrList = updateNetworkACLItemCmd.getSourceCidrList();
        if (!isPartialUpgrade || CollectionUtils.isNotEmpty(sourceCidrList)) {
            networkACLItemVo.setSourceCidrList(sourceCidrList);
        }
        String protocol = updateNetworkACLItemCmd.getProtocol();
        if (!isPartialUpgrade || StringUtils.isNotBlank(protocol)) {
            networkACLItemVo.setProtocol(protocol);
        }
        Integer icmpCode = updateNetworkACLItemCmd.getIcmpCode();
        if (!isPartialUpgrade || icmpCode != null) {
            networkACLItemVo.setIcmpCode(icmpCode);
        }
        Integer icmpType = updateNetworkACLItemCmd.getIcmpType();
        if (!isPartialUpgrade || icmpType != null) {
            networkACLItemVo.setIcmpType(icmpType);
        }
        String action = updateNetworkACLItemCmd.getAction();
        if (!isPartialUpgrade || StringUtils.isNotBlank(action)) {
            Action aclRuleAction = validateAndCreateNetworkAclRuleAction(action);
            networkACLItemVo.setAction(aclRuleAction);
        }
        TrafficType trafficType = updateNetworkACLItemCmd.getTrafficType();
        if (!isPartialUpgrade || trafficType != null) {
            networkACLItemVo.setTrafficType(trafficType);
        }
        String customId = updateNetworkACLItemCmd.getCustomId();
        if (StringUtils.isNotBlank(customId)) {
            networkACLItemVo.setUuid(customId);
        }
        boolean display = updateNetworkACLItemCmd.isDisplay();
        if (!isPartialUpgrade || display != networkACLItemVo.isDisplay()) {
            networkACLItemVo.setDisplay(display);
        }
        String reason = updateNetworkACLItemCmd.getReason();
        if (!isPartialUpgrade || StringUtils.isNotBlank(reason)) {
            networkACLItemVo.setReason(reason);
        }
    }

    /**
     * We validate the network ACL rule ID provided. If not ACL rule is found with the given Id an {@link InvalidParameterValueException} is thrown.
     * If an ACL rule is found, we return the clone of the rule to avoid messing up with CGlib enhanced objects that might be linked to database entries.
     */
    protected NetworkACLItemVO validateNetworkAclRuleIdAndRetrieveIt(UpdateNetworkACLItemCmd updateNetworkACLItemCmd) {
        Long id = updateNetworkACLItemCmd.getId();
        NetworkACLItemVO networkACLItemVoFromDatabase = _networkACLItemDao.findById(id);
        if (networkACLItemVoFromDatabase == null) {
            throw new InvalidParameterValueException(String.format("Unable to find ACL rule with ID [%s]", id));
        }
        return networkACLItemVoFromDatabase.clone();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_UPDATE, eventDescription = "updating network acl", async = true)
    public NetworkACL updateNetworkACL(UpdateNetworkACLListCmd updateNetworkACLListCmd) {
        Long id = updateNetworkACLListCmd.getId();
        NetworkACLVO acl = _networkACLDao.findById(id);
        Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());

        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, vpc);

        String name = updateNetworkACLListCmd.getName();
        if (StringUtils.isNotBlank(name)) {
            acl.setName(name);
        }
        String description = updateNetworkACLListCmd.getDescription();
        if (StringUtils.isNotBlank(description)) {
            acl.setDescription(description);
        }
        String customId = updateNetworkACLListCmd.getCustomId();
        if (StringUtils.isNotBlank(customId)) {
            acl.setUuid(customId);
        }
        Boolean forDisplay = updateNetworkACLListCmd.getDisplay();
        if (forDisplay != null) {
            acl.setDisplay(forDisplay);
        }
        _networkACLDao.update(id, acl);
        return _networkACLDao.findById(id);
    }

    @Override
    public NetworkACLItem moveNetworkAclRuleToNewPosition(MoveNetworkAclItemCmd moveNetworkAclItemCmd) {
        String uuidRuleBeingMoved = moveNetworkAclItemCmd.getUuidRuleBeingMoved();
        String nextAclRuleUuid = moveNetworkAclItemCmd.getNextAclRuleUuid();
        String previousAclRuleUuid = moveNetworkAclItemCmd.getPreviousAclRuleUuid();

        if (StringUtils.isAllBlank(previousAclRuleUuid, nextAclRuleUuid)) {
            throw new InvalidParameterValueException("Both previous and next ACL rule IDs cannot be blank.");
        }

        NetworkACLItemVO ruleBeingMoved = _networkACLItemDao.findByUuid(uuidRuleBeingMoved);
        if (ruleBeingMoved == null) {
            throw new InvalidParameterValueException(String.format("Could not find a rule with ID[%s]", uuidRuleBeingMoved));
        }
        NetworkACLItemVO previousRule = retrieveAndValidateAclRule(previousAclRuleUuid);
        NetworkACLItemVO nextRule = retrieveAndValidateAclRule(nextAclRuleUuid);

        validateMoveAclRulesData(ruleBeingMoved, previousRule, nextRule);

        try {
            NetworkACLVO lockedAcl = _networkACLDao.acquireInLockTable(ruleBeingMoved.getAclId());
            List<NetworkACLItemVO> allAclRules = getAllAclRulesSortedByNumber(lockedAcl.getId());
            validateAclConsistency(moveNetworkAclItemCmd, lockedAcl, allAclRules);

            if (previousRule == null) {
                return moveRuleToTheTop(ruleBeingMoved, allAclRules);
            }
            if (nextRule == null) {
                return moveRuleToTheBottom(ruleBeingMoved, allAclRules);
            }
            return moveRuleBetweenAclRules(ruleBeingMoved, allAclRules, previousRule, nextRule);
        } finally {
            _networkACLDao.releaseFromLockTable(ruleBeingMoved.getAclId());
        }
    }

    /**
     * Validates the consistency of the ACL; the validation process is the following.
     * <ul>
     *  <li> If the ACL does not have rules yet, we do not have any validation to perform;
     *  <li> we will check first if the user provided a consistency hash; if not, we will log a warning message informing administrators that the user is performing the call is assuming the risks of applying ACL replacement without a consistency check;
     *  <li> if the ACL consistency hash is entered by the user, we check if it is the same as we currently have in the database. If it is different we throw an exception.
     * </ul>
     *
     * If the consistency hash sent by the user is the same as the one we get with the database data we should be safe to proceed.
     */
    protected void validateAclConsistency(MoveNetworkAclItemCmd moveNetworkAclItemCmd, NetworkACLVO lockedAcl, List<NetworkACLItemVO> allAclRules) {
        if (CollectionUtils.isEmpty(allAclRules)) {
            s_logger.debug(String.format("No ACL rules for [id=%s, name=%s]. Therefore, there is no need for consistency validation.", lockedAcl.getUuid(), lockedAcl.getName()));
            return;
        }
        String aclConsistencyHash = moveNetworkAclItemCmd.getAclConsistencyHash();
        if (StringUtils.isBlank(aclConsistencyHash)) {
            User callingUser = CallContext.current().getCallingUser();
            Account callingAccount = CallContext.current().getCallingAccount();

            s_logger.warn(String.format(
                    "User [id=%s, name=%s] from Account [id=%s, name=%s] has not entered an ACL consistency hash to execute the replacement of an ACL rule. Therefore, she/he is assuming all of the risks of procedding without this validation.",
                    callingUser.getUuid(), callingUser.getUsername(), callingAccount.getUuid(), callingAccount.getAccountName()));
            return;
        }
        String aclRulesUuids = StringUtils.EMPTY;
        for (NetworkACLItemVO rule : allAclRules) {
            aclRulesUuids += rule.getUuid();
        }
        String md5UuidsSortedByNumber = DigestUtils.md5Hex(aclRulesUuids);
        if (!md5UuidsSortedByNumber.equals(aclConsistencyHash)) {
            throw new InvalidParameterValueException("It seems that the access control list in the database is not in the state that you used to apply the changed. Could you try it again?");
        }
    }

    /**
     * Loads all ACL rules from given network ACL list. Then, the ACL rules will be sorted according to the 'number' field in ascending order.
     */
    protected List<NetworkACLItemVO> getAllAclRulesSortedByNumber(long aclId) {
        List<NetworkACLItemVO> allAclRules = _networkACLItemDao.listByACL(aclId);
        Collections.sort(allAclRules, new Comparator<NetworkACLItemVO>() {
            @Override
            public int compare(NetworkACLItemVO o1, NetworkACLItemVO o2) {
                return o1.number - o2.number;
            }
        });
        return allAclRules;
    }

    /**
     * Moves an ACL to the space between to other rules. If there is already enough room to accommodate the ACL rule being moved, we simply get the 'number' field from the previous ACL rule and add one, and then define this new value as the 'number' value for the ACL rule being moved.
     * Otherwise, we will need to make room. This process is executed via {@link #updateAclRuleToNewPositionAndExecuteShiftIfNecessary(NetworkACLItemVO, int, List, int)}, which will create the space between ACL rules if necessary. This involves shifting ACL rules to accommodate the rule being moved.
     */
    protected NetworkACLItem moveRuleBetweenAclRules(NetworkACLItemVO ruleBeingMoved, List<NetworkACLItemVO> allAclRules, NetworkACLItemVO previousRule, NetworkACLItemVO nextRule) {
        if (previousRule.getNumber() + 1 != nextRule.getNumber()) {
            int newNumberFieldValue = previousRule.getNumber() + 1;
            for (NetworkACLItemVO networkACLItemVO : allAclRules) {
                if (networkACLItemVO.getNumber() == newNumberFieldValue) {
                    throw new InvalidParameterValueException("There are some inconsistencies with the data you sent. The new position calculated already has a ACL rule on it.");
                }
            }
            ruleBeingMoved.setNumber(newNumberFieldValue);
            _networkACLItemDao.updateNumberFieldNetworkItem(ruleBeingMoved.getId(), newNumberFieldValue);
            return _networkACLItemDao.findById(ruleBeingMoved.getId());
        }
        int positionToStartProcessing = 0;
        for (int i = 0; i < allAclRules.size(); i++) {
            if (allAclRules.get(i).getId() == previousRule.getId()) {
                positionToStartProcessing = i + 1;
                break;
            }
        }
        return updateAclRuleToNewPositionAndExecuteShiftIfNecessary(ruleBeingMoved, previousRule.getNumber() + 1, allAclRules, positionToStartProcessing);
    }

    /**
     *  Moves a network ACL rule to the bottom of the list. This is executed by getting the 'number' field of the last ACL rule from the ACL list, and incrementing one.
     *  This new value is assigned to the network ACL being moved and updated in the database using {@link NetworkACLItemDao#updateNumberFieldNetworkItem(long, int)}.
     */
    protected NetworkACLItem moveRuleToTheBottom(NetworkACLItemVO ruleBeingMoved, List<NetworkACLItemVO> allAclRules) {
        NetworkACLItemVO lastAclRule = allAclRules.get(allAclRules.size() - 1);

        int newNumberFieldValue = lastAclRule.getNumber() + 1;
        ruleBeingMoved.setNumber(newNumberFieldValue);

        _networkACLItemDao.updateNumberFieldNetworkItem(ruleBeingMoved.getId(), newNumberFieldValue);
        return _networkACLItemDao.findById(ruleBeingMoved.getId());
    }

    /**
     *  Move the rule to the top of the ACL rule list. This means that the ACL rule being moved will receive the position '1'.
     *  Also, if necessary other ACL rules will have their 'number' field updated to create room for the new top rule.
     */
    protected NetworkACLItem moveRuleToTheTop(NetworkACLItemVO ruleBeingMoved, List<NetworkACLItemVO> allAclRules) {
        return updateAclRuleToNewPositionAndExecuteShiftIfNecessary(ruleBeingMoved, 1, allAclRules, 0);
    }

    /**
     * Updates the ACL rule number executing the shift on subsequent ACL rules if necessary.
     * For example, if we have the following ACL rules:
     * <ul>
     *  <li> ACL A - number 1
     *  <li> ACL B - number 2
     *  <li> ACL C - number 3
     *  <li> ACL D - number 12
     * </ul>
     * If we move 'ACL D' to a place  between 'ACL A' and 'ACL B', this method will execute the shift needded to create the space for 'ACL D'.
     * After applying this method, we will have the following condition.
     * <ul>
     *  <li> ACL A - number 1
     *  <li> ACL D - number 2
     *  <li> ACL B - number 3
     *  <li> ACL C - number 4
     * </ul>
     */
    protected NetworkACLItem updateAclRuleToNewPositionAndExecuteShiftIfNecessary(NetworkACLItemVO ruleBeingMoved, int newNumberFieldValue, List<NetworkACLItemVO> allAclRules,
            int indexToStartProcessing) {
        ruleBeingMoved.setNumber(newNumberFieldValue);
        for (int i = indexToStartProcessing; i < allAclRules.size(); i++) {
            NetworkACLItemVO networkACLItemVO = allAclRules.get(i);
            if (networkACLItemVO.getId() == ruleBeingMoved.getId()) {
                continue;
            }
            if (newNumberFieldValue != networkACLItemVO.getNumber()) {
                break;
            }
            int newNumberFieldValueNextAclRule = newNumberFieldValue + 1;
            updateAclRuleToNewPositionAndExecuteShiftIfNecessary(networkACLItemVO, newNumberFieldValueNextAclRule, allAclRules, i);
        }
        _networkACLItemDao.updateNumberFieldNetworkItem(ruleBeingMoved.getId(), newNumberFieldValue);
        return _networkACLItemDao.findById(ruleBeingMoved.getId());
    }

    /**
     * Searches in the database for an ACL rule by its UUID.
     * An {@link InvalidParameterValueException} is thrown if no ACL rule is found with the given UUID.
     */
    protected NetworkACLItemVO retrieveAndValidateAclRule(String aclRuleUuid) {
        if (StringUtils.isBlank(aclRuleUuid)) {
            return null;
        }
        NetworkACLItemVO aclRule = _networkACLItemDao.findByUuid(aclRuleUuid);
        if (aclRule == null) {
            throw new InvalidParameterValueException(String.format("Could not find rule with ID [%s]", aclRuleUuid));
        }
        return aclRule;
    }

    /**
     *  Validates if the data provided to move the ACL rule is supported by this implementation. The user needs to provide a valid ACL UUID, and at least one of the previous or the next ACL rule.
     *  The validation is as follows:
     *  <ul>
     *      <li> If both ACL rules 'previous' and 'next' are invalid, we throw an {@link InvalidParameterValueException};
     *      <li> informed previous and next ACL rules must have the same ACL ID as the rule being moved; otherwise, an {@link InvalidParameterValueException} is thrown;
     *      <li> then we check if the user trying to move ACL rules has access to the VPC, where the ACL rules are being applied.
     *  </ul>
     */
    protected void validateMoveAclRulesData(NetworkACLItemVO ruleBeingMoved, NetworkACLItemVO previousRule, NetworkACLItemVO nextRule) {
        if (nextRule == null && previousRule == null) {
            throw new InvalidParameterValueException("Both previous and next ACL rule IDs cannot be invalid.");
        }
        long aclId = ruleBeingMoved.getAclId();

        if ((nextRule != null && nextRule.getAclId() != aclId) || (previousRule != null && previousRule.getAclId() != aclId)) {
            throw new InvalidParameterValueException("Cannot use ACL rules from differenting ACLs. Rule being moved.");
        }
        NetworkACLVO acl = _networkACLDao.findById(aclId);
        Vpc vpc = _entityMgr.findById(Vpc.class, acl.getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Re-ordering rules for a default ACL is prohibited");
        }
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, vpc);
    }
}
