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
package com.cloud.bgp;

import com.cloud.dc.ASNumberRangeVO;
import com.cloud.dc.ASNumberVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ASNumberDao;
import com.cloud.dc.dao.ASNumberRangeDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.BgpServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.user.bgp.ListASNumbersCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.BgpPeerVO;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.apache.cloudstack.network.dao.BgpPeerDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BGPServiceImpl implements BGPService {

    public static final Logger LOGGER = LogManager.getLogger(BGPServiceImpl.class);

    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ASNumberRangeDao asNumberRangeDao;
    @Inject
    private ASNumberDao asNumberDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private VpcOfferingDao vpcOfferingDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    private AccountDao accountDao;
    @Inject
    private DomainDao domainDao;
    @Inject
    NetworkServiceMapDao ntwkSrvcDao;
    @Inject
    NetworkModel networkModel;
    @Inject
    BgpPeerDao bgpPeerDao;
    @Inject
    RoutedIpv4Manager routedIpv4Manager;
    @Inject
    VpcServiceMapDao vpcServiceMapDao;

    public BGPServiceImpl() {
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AS_RANGE_CREATE, eventDescription = "AS Range creation")
    public ASNumberRange createASNumberRange(long zoneId, long startASNumber, long endASNumber) {
        DataCenterVO zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            String msg = String.format("Cannot find a zone with ID %s", zoneId);
            LOGGER.error(msg);
            throw new InvalidParameterException(msg);
        }
        if (startASNumber > endASNumber) {
            String msg = "Please specify a valid AS Number range";
            LOGGER.error(msg);
            throw new InvalidParameterException(msg);
        }

        List<ASNumberRangeVO> asNumberRanges = asNumberRangeDao.listByZoneId(zoneId);
        for (ASNumberRangeVO asNumberRange : asNumberRanges) {
            if (isASNumbersOverlap(asNumberRange.getStartASNumber(), asNumberRange.getEndASNumber(), startASNumber, endASNumber)) {
                throw new InvalidParameterException(String.format("New AS number range (%s-%s) has conflict with an existing AS number range (%s-%s)",
                        startASNumber, endASNumber, asNumberRange.getStartASNumber(), asNumberRange.getEndASNumber()));
            }
        }

        try {
            return Transaction.execute((TransactionCallback<ASNumberRange>) status -> {
                LOGGER.debug(String.format("Persisting AS Number Range %s-%s for the zone %s", startASNumber, endASNumber, zone.getName()));
                ASNumberRangeVO asNumberRangeVO = new ASNumberRangeVO(zoneId, startASNumber, endASNumber);
                asNumberRangeDao.persist(asNumberRangeVO);

                for (long asn = startASNumber; asn <= endASNumber; asn++) {
                    LOGGER.debug(String.format("Persisting AS Number %s for zone %s", asn, zone.getName()));
                    ASNumberVO asNumber = new ASNumberVO(asn, asNumberRangeVO.getId(), zoneId);
                    asNumberDao.persist(asNumber);
                }
                return asNumberRangeVO;
            });
        } catch (Exception e) {
            String err = String.format("Error creating AS Number range %s-%s for zone %s: %s", startASNumber, endASNumber, zone.getName(), e.getMessage());
            LOGGER.error(err, e);
            throw new CloudRuntimeException(err);
        }
    }

    protected boolean isASNumbersOverlap(long startNumber1, long endNumber1, long startNumber2, long endNumber2) {
        if (startNumber1 > endNumber2 || startNumber2 > endNumber1) {
            return false;
        }
        return true;
    }

    @Override
    public List<ASNumberRange> listASNumberRanges(Long zoneId) {
        List<ASNumberRangeVO> rangeVOList = zoneId != null ? asNumberRangeDao.listByZoneId(zoneId) : asNumberRangeDao.listAll();
        return new ArrayList<>(rangeVOList);
    }

    @Override
    public Pair<List<ASNumber>, Integer> listASNumbers(ListASNumbersCmd cmd) {
        Long zoneId = cmd.getZoneId();
        Long asNumberRangeId = cmd.getAsNumberRangeId();
        Integer asNumber = cmd.getAsNumber();
        Boolean allocated = cmd.getAllocated();
        Long networkId = cmd.getNetworkId();
        Long vpcId = cmd.getVpcId();
        String accountName = cmd.getAccount();
        Long domainId = cmd.getDomainId();
        Long startIndex = cmd.getStartIndex();
        Long pageSizeVal = cmd.getPageSizeVal();
        String keyword = cmd.getKeyword();

        Account userAccount = null;
        Domain domain = null;
        final Account caller = CallContext.current().getCallingAccount();
        if (Objects.nonNull(accountName)) {
            if (domainId != null) {
                userAccount = accountDao.findActiveAccount(accountName, domainId);
                domain = domainDao.findById(domainId);
            } else {
                userAccount = accountDao.findActiveAccount(accountName, caller.getDomainId());
                domain = domainDao.findById(caller.getDomainId());
            }
        }

        if (Objects.nonNull(accountName) && Objects.isNull(userAccount)) {
            throw new InvalidParameterException(String.format("Failed to find user Account: %s", accountName));
        }

        Long networkSearchId = networkId;
        Long vpcSerchId = vpcId;
        if (networkId != null) {
            NetworkVO network = networkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterException(String.format("Failed to find network with ID: %s", networkId));
            }
            if (network.getVpcId() != null) {
                LOGGER.debug(String.format("The network %s is a VPC tier, searching for the AS number on the VPC with ID %s",
                        network.getName(), network.getVpcId()));
                networkSearchId = null;
                vpcSerchId = network.getVpcId();
            }
        }
        Pair<List<ASNumberVO>, Integer> pair = asNumberDao.searchAndCountByZoneOrRangeOrAllocated(zoneId, asNumberRangeId,
                asNumber, networkSearchId, vpcSerchId, allocated, Objects.nonNull(userAccount) ? userAccount.getId() : null,
                Objects.nonNull(domain) ? domain.getId() : null, keyword, caller, startIndex, pageSizeVal);
        return new Pair<>(new ArrayList<>(pair.first()), pair.second());
    }

    @Override
    public boolean allocateASNumber(long zoneId, Long asNumber, Long networkId, Long vpcId) {
        ASNumberVO asNumberVO = isOfferingSpecifyAsNumber(networkId, vpcId) ?
                asNumberDao.findByAsNumber(asNumber) :
                asNumberDao.findOneByAllocationStateAndZone(zoneId, false);
        if (asNumberVO == null || asNumberVO.getDataCenterId() != zoneId) {
            if (asNumber != null) {
                LOGGER.error(String.format("Cannot find AS number %s in zone with ID %s", asNumber, zoneId));
                return false;
            }
            throw new CloudRuntimeException(String.format("Cannot allocate AS number in zone with ID %s", zoneId));
        }
        long accountId, domainId;
        String netName;
        if (Objects.nonNull(vpcId)) {
            VpcVO vpc = vpcDao.findById(vpcId);
            if (vpc == null) {
                LOGGER.error(String.format("Cannot find VPC with ID %s", vpcId));
                return false;
            }
            accountId = vpc.getAccountId();
            domainId = vpc.getDomainId();
            netName = vpc.getName();
        } else {
            NetworkVO network = networkDao.findById(networkId);
            if (network == null) {
                LOGGER.error(String.format("Cannot find network with ID %s", networkId));
                return false;
            }
            accountId = network.getAccountId();
            domainId = network.getDomainId();
            netName = network.getName();
        }

        LOGGER.debug(String.format("Allocating the AS Number %s to %s %s on zone %s", asNumber,
                (Objects.nonNull(vpcId) ? "VPC" : "network"), netName, zoneId));
        asNumberVO.setAllocated(true);
        asNumberVO.setAllocatedTime(new Date());
        if (Objects.nonNull(vpcId)) {
            asNumberVO.setVpcId(vpcId);
        } else {
            asNumberVO.setNetworkId(networkId);
        }
        asNumberVO.setAccountId(accountId);
        asNumberVO.setDomainId(domainId);
        return asNumberDao.update(asNumberVO.getId(), asNumberVO);
    }

    private boolean isOfferingSpecifyAsNumber(Long networkId, Long vpcId) {
        if (Objects.nonNull(vpcId)) {
            VpcVO vpc = vpcDao.findById(vpcId);
            if (vpc == null) {
                throw new CloudRuntimeException(String.format("Cannot find VPC with ID %s", vpcId));
            }
            VpcOfferingVO vpcOffering = vpcOfferingDao.findById(vpc.getVpcOfferingId());
            return NetworkOffering.RoutingMode.Dynamic == vpcOffering.getRoutingMode() && BooleanUtils.toBoolean(vpcOffering.isSpecifyAsNumber());
        } else {
            NetworkVO network = networkDao.findById(networkId);
            NetworkOfferingVO networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
            return NetworkOffering.RoutingMode.Dynamic == networkOffering.getRoutingMode() && BooleanUtils.toBoolean(networkOffering.isSpecifyAsNumber());
        }
    }

    private Pair<Boolean, String> logAndReturnErrorMessage(String msg) {
        LOGGER.error(msg);
        return new Pair<>(false, msg);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AS_NUMBER_RELEASE, eventDescription = "Releasing AS Number")
    public Pair<Boolean, String> releaseASNumber(long zoneId, long asNumber, boolean isDestroyNetworkOperation) {
        ASNumberVO asNumberVO = asNumberDao.findByAsNumber(asNumber);
        if (asNumberVO == null) {
            return logAndReturnErrorMessage(String.format("Cannot find AS Number %s on zone %s", asNumber, zoneId));
        }
        if (!asNumberVO.isAllocated()) {
            LOGGER.debug(String.format("The AS Number %s is not allocated to any network on zone %s, ignoring release", asNumber, zoneId));
            return new Pair<>(true, "");
        }
        Long networkId = asNumberVO.getNetworkId();
        Long vpcId = asNumberVO.getVpcId();
        if (!isDestroyNetworkOperation) {
            Pair<Boolean, String> checksResult = performReleaseASNumberChecks(networkId, vpcId, asNumber);
            if (checksResult != null) {
                return checksResult;
            }
        }
        LOGGER.debug(String.format("Releasing AS Number %s on zone %s from previous allocation", asNumber, zoneId));
        asNumberVO.setAllocated(false);
        asNumberVO.setAllocatedTime(null);
        asNumberVO.setDomainId(null);
        asNumberVO.setAccountId(null);
        if (vpcId != null) {
            asNumberVO.setVpcId(null);
        } else {
            asNumberVO.setNetworkId(null);
        }
        boolean update = asNumberDao.update(asNumberVO.getId(), asNumberVO);
        String msg = update ? "OK" : "Could not update database record for AS number";
        return new Pair<>(update, msg);
    }

    private Pair<Boolean, String> performReleaseASNumberChecks(Long networkId, Long vpcId, long asNumber) {
        if (networkId != null) {
            NetworkVO network = networkDao.findById(networkId);
            if (network == null) {
                return logAndReturnErrorMessage(String.format("Cannot find a network with ID %s which acquired the AS number %s", networkId, asNumber));
            }
            NetworkOfferingVO offering = networkOfferingDao.findById(network.getNetworkOfferingId());
            if (offering == null) {
                return logAndReturnErrorMessage(String.format("Cannot find a network offering with ID %s", network.getNetworkOfferingId()));
            }
            if (offering.isSpecifyAsNumber()) {
                return logAndReturnErrorMessage(String.format("Cannot release the AS number %s as it is acquired by a network that requires AS number", asNumber));
            }
        } else if (vpcId != null) {
            VpcVO vpc = vpcDao.findById(vpcId);
            if (vpc == null) {
                return logAndReturnErrorMessage(String.format("Cannot find a VPC with ID %s which acquired the AS number %s", vpcId, asNumber));
            }
            VpcOfferingVO vpcOffering = vpcOfferingDao.findById(vpc.getVpcOfferingId());
            if (vpcOffering == null) {
                return logAndReturnErrorMessage(String.format("Cannot find a VPC offering with ID %s", vpc.getVpcOfferingId()));
            }
            if (vpcOffering.isSpecifyAsNumber()) {
                return logAndReturnErrorMessage(String.format("Cannot release the AS number %s as it is acquired by a VPC that requires AS number", asNumber));
            }
        }
        return null;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AS_RANGE_DELETE, eventDescription = "Deleting AS Range")
    public boolean deleteASRange(long id) {
        final ASNumberRange asRange = asNumberRangeDao.findById(id);
        if (asRange == null) {
            throw new CloudRuntimeException(String.format("Could not find a AS range with id: %s", id));
        }
        long startASNumber = asRange.getStartASNumber();
        long endASNumber = asRange.getEndASNumber();
        long zoneId = asRange.getDataCenterId();
        List<ASNumberVO> allocatedAsNumbers = asNumberDao.listAllocatedByASRange(asRange.getId());
        if (Objects.nonNull(allocatedAsNumbers) && !allocatedAsNumbers.isEmpty()) {
            throw new CloudRuntimeException(String.format("There are %s AS numbers in use from the range %s-%s, cannot remove the range",
                    allocatedAsNumbers.size(), startASNumber, endASNumber));
        }
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    int removedASNumbers = asNumberDao.removeASRangeNumbers(asRange.getId());
                    LOGGER.debug(String.format("Removed %s AS numbers from the range %s-%s", removedASNumbers,
                            startASNumber, endASNumber));
                    asNumberRangeDao.remove(id);
                    LOGGER.debug(String.format("Removing the AS Number Range %s-%s for the zone %s", startASNumber,
                            endASNumber, zoneId));
                }
            });
        } catch (Exception e) {
            String err = String.format("Error removing AS Number range %s-%s for zone %s: %s",
                    startASNumber, endASNumber, zoneId, e.getMessage());
            LOGGER.error(err, e);
            throw new CloudRuntimeException(err);
        }
        return true;
    }

    @Override
    public boolean applyBgpPeers(Network network, boolean continueOnError) throws ResourceUnavailableException {
        if (!routedIpv4Manager.isDynamicRoutedNetwork(network)) {
            return true;
        }
        final String gatewayProviderStr = ntwkSrvcDao.getProviderForServiceInNetwork(network.getId(), Network.Service.Gateway);
        if (gatewayProviderStr != null) {
            NetworkElement provider = networkModel.getElementImplementingProvider(gatewayProviderStr);
            if (provider != null && provider instanceof BgpServiceProvider) {
                List<BgpPeerVO> bgpPeers;
                if (network.getVpcId() != null) {
                    bgpPeers = bgpPeerDao.listNonRevokeByVpcId(network.getVpcId());
                } else {
                    bgpPeers = bgpPeerDao.listNonRevokeByNetworkId(network.getId());
                }
                if (CollectionUtils.isEmpty(bgpPeers)) {
                    Account owner = accountDao.findByIdIncludingRemoved(network.getAccountId());
                    List<Long> bgpPeerIds = routedIpv4Manager.getBgpPeerIdsForAccount(owner, network.getDataCenterId());
                    bgpPeers = bgpPeerIds.stream()
                            .map(bgpPeerId -> bgpPeerDao.findById(bgpPeerId))
                            .collect(Collectors.toList());
                }
                LOGGER.debug(String.format("Applying BPG Peers for network [%s]: [%s]", network, bgpPeers));
                return ((BgpServiceProvider) provider).applyBgpPeers(null, network, bgpPeers);
            }
        }
        return true;
    }

    @Override
    public boolean applyBgpPeers(Vpc vpc, boolean continueOnError) throws ResourceUnavailableException {
        if (!routedIpv4Manager.isDynamicRoutedVpc(vpc)) {
            return true;
        }
        final String gatewayProviderStr = vpcServiceMapDao.getProviderForServiceInVpc(vpc.getId(), Network.Service.Gateway);
        if (gatewayProviderStr != null) {
            NetworkElement provider = networkModel.getElementImplementingProvider(gatewayProviderStr);
            if (provider != null && provider instanceof BgpServiceProvider) {
                List<BgpPeerVO> bgpPeers = bgpPeerDao.listNonRevokeByVpcId(vpc.getId());
                if (CollectionUtils.isEmpty(bgpPeers)) {
                    Account owner = accountDao.findByIdIncludingRemoved(vpc.getAccountId());
                    List<Long> bgpPeerIds = routedIpv4Manager.getBgpPeerIdsForAccount(owner, vpc.getZoneId());
                    bgpPeers = bgpPeerIds.stream()
                            .map(bgpPeerId -> bgpPeerDao.findById(bgpPeerId))
                            .collect(Collectors.toList());
                }
                LOGGER.debug(String.format("Applying BPG Peers for VPC [%s]: [%s]", vpc, bgpPeers));
                return ((BgpServiceProvider) provider).applyBgpPeers(vpc, null, bgpPeers);

            }
        }
        return true;
    }
}
