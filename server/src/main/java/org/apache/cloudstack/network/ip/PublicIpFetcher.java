package org.apache.cloudstack.network.ip;

import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Resource;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DomainVlanMapVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DomainVlanMapDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.rules.FirewallManager;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PublicIpFetcher {
    private static final Logger LOG = Logger.getLogger(PublicIpFetcher.class);
    private final IpAddressManagerImpl ipAddressManagerImpl;

    SearchBuilder<IPAddressVO> AssignIpAddressSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressFromPodVlanSearch;

    @Inject
    AccountVlanMapDao accountVlanMapDao;
    @Inject
    DomainVlanMapDao domainVlanMapDao;
    @Inject
    EntityManager entityMgr;
    @Inject
    FirewallManager firewallMgr;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    NetworkDao networksDao;
    @Inject
    PodVlanMapDao podVlanMapDao;
    @Inject
    ResourceLimitService resourceLimitMgr;
    @Inject
    VlanDao vlanDao;

    public PublicIpFetcher(IpAddressManagerImpl ipAddressManagerImpl) {
        this.ipAddressManagerImpl = ipAddressManagerImpl;

        createAssignIpAddressSearch();

        createAssignIpAddressFromPodVlanSearch();
    }

    private void createAssignIpAddressFromPodVlanSearch() {
        AssignIpAddressFromPodVlanSearch = ipAddressDao.createSearchBuilder();
        AssignIpAddressFromPodVlanSearch.and("dc", AssignIpAddressFromPodVlanSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AssignIpAddressFromPodVlanSearch.and("allocated", AssignIpAddressFromPodVlanSearch.entity().getAllocatedTime(), SearchCriteria.Op.NULL);
        AssignIpAddressFromPodVlanSearch.and("vlanId", AssignIpAddressFromPodVlanSearch.entity().getVlanId(), SearchCriteria.Op.IN);
        SearchBuilder<VlanVO> podVlanSearch = vlanDao.createSearchBuilder();
        podVlanSearch.and("type", podVlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        podVlanSearch.and("networkId", podVlanSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        SearchBuilder<PodVlanMapVO> podVlanMapSB = podVlanMapDao.createSearchBuilder();
        podVlanMapSB.and("podId", podVlanMapSB.entity().getPodId(), SearchCriteria.Op.EQ);
        AssignIpAddressFromPodVlanSearch.join("podVlanMapSB", podVlanMapSB, podVlanMapSB.entity().getVlanDbId(), AssignIpAddressFromPodVlanSearch.entity().getVlanId(),
                JoinBuilder.JoinType.INNER);
        AssignIpAddressFromPodVlanSearch.join("vlan", podVlanSearch, podVlanSearch.entity().getId(), AssignIpAddressFromPodVlanSearch.entity().getVlanId(), JoinBuilder.JoinType.INNER);
        AssignIpAddressFromPodVlanSearch.done();
    }

    private void createAssignIpAddressSearch() {
        AssignIpAddressSearch = ipAddressDao.createSearchBuilder();
        AssignIpAddressSearch.and("dc", AssignIpAddressSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AssignIpAddressSearch.and("allocated", AssignIpAddressSearch.entity().getAllocatedTime(), SearchCriteria.Op.NULL);
        AssignIpAddressSearch.and("vlanId", AssignIpAddressSearch.entity().getVlanId(), SearchCriteria.Op.IN);
        if (ipAddressManagerImpl.getSystemVmPublicIpReservationModeStrictness()) {
            AssignIpAddressSearch.and("forSystemVms", AssignIpAddressSearch.entity().isForSystemVms(), SearchCriteria.Op.EQ);
        }
        SearchBuilder<VlanVO> vlanSearch = vlanDao.createSearchBuilder();
        vlanSearch.and("type", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        vlanSearch.and("networkId", vlanSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AssignIpAddressSearch.join("vlan", vlanSearch, vlanSearch.entity().getId(), AssignIpAddressSearch.entity().getVlanId(), JoinBuilder.JoinType.INNER);
        AssignIpAddressSearch.done();
    }

    @DB public PublicIp fetchNewPublicIp(final long dcId, final Long podId, final List<Long> vlanDbIds, final Account owner, final Vlan.VlanType vlanUse, final Long guestNetworkId,
            final boolean sourceNat, final boolean assign, final String requestedIp, final boolean isSystem, final Long vpcId, final Boolean displayIp, final boolean forSystemVms)
            throws InsufficientAddressCapacityException {
        IPAddressVO addr = Transaction.execute(new TransactionCallbackWithException<IPAddressVO, InsufficientAddressCapacityException>() {
            @Override public IPAddressVO doInTransaction(TransactionStatus status) throws InsufficientAddressCapacityException {
                StringBuilder errorMessage = new StringBuilder("Unable to get ip address in ");
                Boolean fetchFromDedicatedRange = false;
                List<Long> dedicatedVlanDbIds = new ArrayList<Long>();
                List<Long> nonDedicatedVlanDbIds = new ArrayList<Long>();
                DataCenter zone = entityMgr.findById(DataCenter.class, dcId);

                SearchCriteria<IPAddressVO> ipAddressSearchCriteria = createIpAddressSearchCriteria(errorMessage, podId, dcId);

                // If owner has dedicated Public IP ranges, fetch IP from the dedicated range
                // Otherwise fetch IP from the system pool
                Network network = networksDao.findById(guestNetworkId);
                //Checking if network is null in the case of system VM's. At the time of allocation of IP address to systemVm, no network is present.
                if (network == null || !(network.getGuestType() == Network.GuestType.Shared && zone.getNetworkType() == DataCenter.NetworkType.Advanced)) {
                    List<AccountVlanMapVO> maps = accountVlanMapDao.listAccountVlanMapsByAccount(owner.getId());
                    for (AccountVlanMapVO map : maps) {
                        if (vlanDbIds == null || vlanDbIds.contains(map.getVlanDbId()))
                            dedicatedVlanDbIds.add(map.getVlanDbId());
                    }
                }
                List<DomainVlanMapVO> domainMaps = domainVlanMapDao.listDomainVlanMapsByDomain(owner.getDomainId());
                for (DomainVlanMapVO map : domainMaps) {
                    if (vlanDbIds == null || vlanDbIds.contains(map.getVlanDbId()))
                        dedicatedVlanDbIds.add(map.getVlanDbId());
                }
                List<VlanVO> nonDedicatedVlans = vlanDao.listZoneWideNonDedicatedVlans(dcId);
                for (VlanVO nonDedicatedVlan : nonDedicatedVlans) {
                    if (vlanDbIds == null || vlanDbIds.contains(nonDedicatedVlan.getId()))
                        nonDedicatedVlanDbIds.add(nonDedicatedVlan.getId());
                }
                if (dedicatedVlanDbIds != null && !dedicatedVlanDbIds.isEmpty()) {
                    fetchFromDedicatedRange = true;
                    ipAddressSearchCriteria.setParameters("vlanId", dedicatedVlanDbIds.toArray());
                    errorMessage.append(", vlanId id=" + Arrays.toString(dedicatedVlanDbIds.toArray()));
                } else if (nonDedicatedVlanDbIds != null && !nonDedicatedVlanDbIds.isEmpty()) {
                    ipAddressSearchCriteria.setParameters("vlanId", nonDedicatedVlanDbIds.toArray());
                    errorMessage.append(", vlanId id=" + Arrays.toString(nonDedicatedVlanDbIds.toArray()));
                } else {
                    thrownInsufficientCapacityInScope(errorMessage, podId, dcId);
                }

                ipAddressSearchCriteria.setParameters("dc", dcId);

                setVlanSearchParameters(errorMessage, ipAddressSearchCriteria, vlanUse, guestNetworkId);

                if (requestedIp != null) {
                    ipAddressSearchCriteria.addAnd("address", SearchCriteria.Op.EQ, requestedIp);
                    errorMessage.append(": requested ip " + requestedIp + " is not available");
                }

                Filter filter = getFilter(ipAddressSearchCriteria, forSystemVms);

                List<IPAddressVO> addrs = ipAddressDao.search(ipAddressSearchCriteria, filter, false);

                // If all the dedicated IPs of the owner are in use fetch an IP from the system pool
                if (addrs.size() == 0 && fetchFromDedicatedRange) {
                    // Verify if account is allowed to acquire IPs from the system
                    boolean useSystemIps = IpAddressManagerImpl.UseSystemPublicIps.valueIn(owner.getId());
                    if (useSystemIps && nonDedicatedVlanDbIds != null && !nonDedicatedVlanDbIds.isEmpty()) {
                        fetchFromDedicatedRange = false;
                        ipAddressSearchCriteria.setParameters("vlanId", nonDedicatedVlanDbIds.toArray());
                        errorMessage.append(", vlanId id=" + Arrays.toString(nonDedicatedVlanDbIds.toArray()));
                        addrs = ipAddressDao.search(ipAddressSearchCriteria, filter, false);
                    }
                }

                checkForNoAddress(errorMessage, addrs, podId, dcId);

                throwExceptionIf(addrs.size() == 1, "Return size is incorrect: " + addrs.size());

                if (!fetchFromDedicatedRange && Vlan.VlanType.VirtualNetwork.equals(vlanUse)) {
                    // Check that the maximum number of public IPs for the given accountId will not be exceeded
                    try {
                        resourceLimitMgr.checkResourceLimit(owner, Resource.ResourceType.public_ip);
                    } catch (ResourceAllocationException ex) {
                        LOG.warn("Failed to allocate resource of type " + ex.getResourceType() + " for account " + owner);
                        throw new AccountLimitException("Maximum number of public IP addresses for account: " + owner.getAccountName() + " has been exceeded.");
                    }
                }

                IPAddressVO finalAddr = null;
                for (final IPAddressVO possibleAddr : addrs) {
                    if (possibleAddr.getState() != IpAddress.State.Free) {
                        continue;
                    }

                    final IPAddressVO addr;
                    addr = createIpAddressWithCorrectProperties(possibleAddr,
                            sourceNat, owner, isSystem, displayIp, vlanUse, guestNetworkId, vpcId);

                    finalAddr = getIpAddressVO(addr);
                    if (finalAddr != null) {
                        break;
                    }
                }

                throwExceptionIf(finalAddr == null, "Failed to fetch any free public IP address");

                if (assign) {
                    ipAddressManagerImpl.markPublicIpAsAllocated(finalAddr);
                }

                validateExpectedAddressState(finalAddr, assign);

                return finalAddr;
            }
        });

        if (vlanUse == Vlan.VlanType.VirtualNetwork) {
            firewallMgr.addSystemFirewallRules(addr, owner);
        }

        return PublicIp.createFromAddrAndVlan(addr, vlanDao.findById(addr.getVlanId()));
    }

    private void checkForNoAddress(StringBuilder errorMessage, List<IPAddressVO> addrs, Long podId, long dcId) throws InsufficientAddressCapacityException {
        if (addrs.size() == 0) {
            thrownInsufficientCapacityInScope(errorMessage, podId, dcId);
            return;
        }
    }

    private void thrownInsufficientCapacityInScope(StringBuilder errorMessage, Long podId, long dcId) throws InsufficientAddressCapacityException {
        if (podId != null) {
            InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", Pod.class, podId);
            ex.addProxyObject(ApiDBUtils.findPodById(podId).getUuid());
            throw ex;
        }
        LOG.warn(errorMessage.toString());
        InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, dcId);
        ex.addProxyObject(ApiDBUtils.findZoneById(dcId).getUuid());
        throw ex;
    }

    private IPAddressVO createIpAddressWithCorrectProperties(IPAddressVO possibleAddr, boolean sourceNat, Account owner, boolean isSystem, Boolean displayIp, Vlan.VlanType vlanUse,
            Long guestNetworkId, Long vpcId) {
        final IPAddressVO addr = possibleAddr;
        addr.setSourceNat(sourceNat);
        addr.setAllocatedTime(new Date());
        addr.setAllocatedInDomainId(owner.getDomainId());
        addr.setAllocatedToAccountId(owner.getId());
        addr.setSystem(isSystem);

        if (displayIp != null) {
            addr.setDisplay(displayIp);
        }

        if (vlanUse != Vlan.VlanType.DirectAttached) {
            addr.setAssociatedWithNetworkId(guestNetworkId);
            addr.setVpcId(vpcId);
        }
        return addr;
    }

    private void setVlanSearchParameters(StringBuilder errorMessage, SearchCriteria<IPAddressVO> ipAddressSearchCriteria, Vlan.VlanType vlanUse, Long guestNetworkId) {
        // for direct network take ip addresses only from the vlans belonging to the network
        if (vlanUse == Vlan.VlanType.DirectAttached) {
            ipAddressSearchCriteria.setJoinParameters("vlan", "networkId", guestNetworkId);
            errorMessage.append(", network id=" + guestNetworkId);
        }
        ipAddressSearchCriteria.setJoinParameters("vlan", "type", vlanUse);
    }

    private SearchCriteria<IPAddressVO> createIpAddressSearchCriteria(StringBuilder errorMessage, Long podId, long dcId) {
        SearchCriteria<IPAddressVO> ipAddressSearchCriteria = null;
        if (podId != null) {
            ipAddressSearchCriteria = AssignIpAddressFromPodVlanSearch.create();
            ipAddressSearchCriteria.setJoinParameters("podVlanMapSB", "podId", podId);
            errorMessage.append(" pod id=" + podId);
        } else {
            ipAddressSearchCriteria = AssignIpAddressSearch.create();
            errorMessage.append(" zone id=" + dcId);
        }
        return ipAddressSearchCriteria;
    }

    private Filter getFilter(SearchCriteria<IPAddressVO> ipAddressSearchCriteria, boolean forSystemVms) {
        boolean ascOrder = !forSystemVms;
        Filter filter = new Filter(IPAddressVO.class, "forSystemVms", ascOrder, 0l, 1l);
        if (ipAddressManagerImpl.getSystemVmPublicIpReservationModeStrictness()) {
            ipAddressSearchCriteria.setParameters("forSystemVms", forSystemVms);
        }

        filter.addOrderBy(IPAddressVO.class, "vlanId", true);
        return filter;
    }

    private IPAddressVO getIpAddressVO(IPAddressVO addr) {
        IPAddressVO finalAddr = null;
        if(LOG.isDebugEnabled()) {
            VlanVO vlanVo = vlanDao.findById(addr.getVlanId());
            LOG.debug(String.format("trying to get %s for vlan(%s)", addr.getAddress().addr(), vlanVo.getVlanTag()));
        }
        if (ipAddressDao.lockRow(addr.getId(), true) != null) {
            final IPAddressVO userIp = ipAddressDao.findById(addr.getId());
            if (userIp.getState() == IpAddress.State.Free) {
                addr.setState(IpAddress.State.Allocating);
                if (ipAddressDao.update(addr.getId(), addr)) {
                    finalAddr = addr;
                }
            }
        } else {
            if(LOG.isInfoEnabled()) {
                LOG.info("acquiring lock for " + addr.getAddress().addr() + " failed");
            }
        }
        return finalAddr;
    }

    private void throwExceptionIf(boolean b, String s) {
        if (b) {
            LOG.error(s);
            throw new CloudRuntimeException(s);
        }
    }

    private void validateExpectedAddressState(IPAddressVO finalAddr, boolean assign) {
        final IpAddress.State expectedAddressState = assign ? IpAddress.State.Allocated : IpAddress.State.Allocating;
        throwExceptionIf(finalAddr.getState() != expectedAddressState, "Failed to fetch new public IP and get in expected state=" + expectedAddressState);
    }
}