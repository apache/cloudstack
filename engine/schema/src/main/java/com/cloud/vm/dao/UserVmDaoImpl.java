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
package com.cloud.vm.dao;

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmData.NicData;
import com.cloud.vm.dao.UserVmData.SecurityGroupData;
import org.apache.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class UserVmDaoImpl extends GenericDaoBase<UserVmVO, Long> implements UserVmDao {
    public static final Logger s_logger = Logger.getLogger(UserVmDaoImpl.class);

    protected SearchBuilder<UserVmVO> AccountPodSearch;
    protected SearchBuilder<UserVmVO> AccountDataCenterSearch;
    protected SearchBuilder<UserVmVO> AccountSearch;
    protected SearchBuilder<UserVmVO> HostSearch;
    protected SearchBuilder<UserVmVO> LastHostSearch;
    protected SearchBuilder<UserVmVO> HostUpSearch;
    protected SearchBuilder<UserVmVO> HostRunningSearch;
    protected SearchBuilder<UserVmVO> StateChangeSearch;
    protected SearchBuilder<UserVmVO> AccountHostSearch;

    protected SearchBuilder<UserVmVO> DestroySearch;
    protected SearchBuilder<UserVmVO> AccountDataCenterVirtualSearch;
    protected GenericSearchBuilder<UserVmVO, Long> CountByAccountPod;
    protected GenericSearchBuilder<UserVmVO, Long> CountByAccount;
    protected GenericSearchBuilder<UserVmVO, Long> PodsHavingVmsForAccount;

    protected SearchBuilder<UserVmVO> UserVmSearch;
    protected SearchBuilder<UserVmVO> UserVmByIsoSearch;
    protected Attribute _updateTimeAttr;
    // ResourceTagsDaoImpl _tagsDao = ComponentLocator.inject(ResourceTagsDaoImpl.class);
    @Inject
    ResourceTagDao _tagsDao;
    @Inject
    NetworkDao networkDao;

    private static final String LIST_PODS_HAVING_VMS_FOR_ACCOUNT =
            "SELECT pod_id FROM cloud.vm_instance WHERE data_center_id = ? AND account_id = ? AND pod_id IS NOT NULL AND (state = 'Running' OR state = 'Stopped') "
                    + "GROUP BY pod_id HAVING count(id) > 0 ORDER BY count(id) DESC";

    private static final String VM_DETAILS = "select vm_instance.id, "
            + "account.id, account.account_name, account.type, domain.name, instance_group.id, instance_group.name,"
            + "data_center.id, data_center.name, data_center.is_security_group_enabled, host.id, host.name, "
            + "vm_template.id, vm_template.name, vm_template.display_text, iso.id, iso.name, "
            + "vm_template.enable_password, service_offering.id, disk_offering.name, storage_pool.id, storage_pool.pool_type, "
            + "service_offering.cpu, service_offering.speed, service_offering.ram_size, volumes.id, volumes.device_id, volumes.volume_type, security_group.id, security_group.name, "
            + "security_group.description, nics.id, nics.ip4_address, nics.default_nic, nics.gateway, nics.network_id, nics.netmask, nics.mac_address, nics.broadcast_uri, " +
            "nics.isolation_uri, "
            + "networks.traffic_type, networks.guest_type, user_ip_address.id, user_ip_address.public_ip_address from vm_instance "
            + "left join account on vm_instance.account_id=account.id  " + "left join domain on vm_instance.domain_id=domain.id "
            + "left join instance_group_vm_map on vm_instance.id=instance_group_vm_map.instance_id "
            + "left join instance_group on instance_group_vm_map.group_id=instance_group.id " + "left join data_center on vm_instance.data_center_id=data_center.id "
            + "left join host on vm_instance.host_id=host.id " + "left join vm_template on vm_instance.vm_template_id=vm_template.id "
            + "left join user_vm on vm_instance.id=user_vm.id " + "left join vm_template iso on iso.id=user_vm.iso_id "
            + "left join service_offering on vm_instance.service_offering_id=service_offering.id "
            + "left join disk_offering  on vm_instance.service_offering_id=disk_offering.id " + "left join volumes on vm_instance.id=volumes.instance_id "
            + "left join storage_pool on volumes.pool_id=storage_pool.id " + "left join security_group_vm_map on vm_instance.id=security_group_vm_map.instance_id "
            + "left join security_group on security_group_vm_map.security_group_id=security_group.id " + "left join nics on vm_instance.id=nics.instance_id "
            + "left join networks on nics.network_id=networks.id " + "left join user_ip_address on user_ip_address.vm_id=vm_instance.id " + "where vm_instance.id in (";

    private static final String VMS_DETAIL_BY_NAME = "select vm_instance.instance_name, vm_instance.vm_type, vm_instance.id , user_vm_details.value, user_vm_details.name from vm_instance "
            + "left join user_vm_details on vm_instance.id = user_vm_details.vm_id where (user_vm_details.name is null or user_vm_details.name = ? ) and vm_instance.instance_name in (";

    private static final int VM_DETAILS_BATCH_SIZE = 100;

    @Inject
    protected UserVmDetailsDao _detailsDao;
    @Inject
    protected NicDao _nicDao;

    public UserVmDaoImpl() {
    }

    @PostConstruct
    void init() {
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        LastHostSearch = createSearchBuilder();
        LastHostSearch.and("lastHost", LastHostSearch.entity().getLastHostId(), SearchCriteria.Op.EQ);
        LastHostSearch.and("state", LastHostSearch.entity().getState(), SearchCriteria.Op.EQ);
        LastHostSearch.done();

        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), SearchCriteria.Op.NIN);
        HostUpSearch.done();

        HostRunningSearch = createSearchBuilder();
        HostRunningSearch.and("host", HostRunningSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostRunningSearch.and("state", HostRunningSearch.entity().getState(), SearchCriteria.Op.EQ);
        HostRunningSearch.done();

        AccountPodSearch = createSearchBuilder();
        AccountPodSearch.and("account", AccountPodSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountPodSearch.and("pod", AccountPodSearch.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        AccountPodSearch.done();

        AccountDataCenterSearch = createSearchBuilder();
        AccountDataCenterSearch.and("account", AccountDataCenterSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountDataCenterSearch.and("dc", AccountDataCenterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountDataCenterSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();

        DestroySearch = createSearchBuilder();
        DestroySearch.and("state", DestroySearch.entity().getState(), SearchCriteria.Op.IN);
        DestroySearch.and("updateTime", DestroySearch.entity().getUpdateTime(), SearchCriteria.Op.LT);
        DestroySearch.done();

        AccountHostSearch = createSearchBuilder();
        AccountHostSearch.and("accountId", AccountHostSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountHostSearch.and("hostId", AccountHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        AccountHostSearch.done();

        CountByAccountPod = createSearchBuilder(Long.class);
        CountByAccountPod.select(null, Func.COUNT, null);
        CountByAccountPod.and("account", CountByAccountPod.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountByAccountPod.and("pod", CountByAccountPod.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        CountByAccountPod.done();

        CountByAccount = createSearchBuilder(Long.class);
        CountByAccount.select(null, Func.COUNT, null);
        CountByAccount.and("account", CountByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountByAccount.and("type", CountByAccount.entity().getType(), SearchCriteria.Op.EQ);
        CountByAccount.and("state", CountByAccount.entity().getState(), SearchCriteria.Op.NIN);
        CountByAccount.and("displayVm", CountByAccount.entity().isDisplayVm(), SearchCriteria.Op.EQ);
        CountByAccount.done();

        SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
        nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        nicSearch.and("ip4Address", nicSearch.entity().getIPv4Address(), SearchCriteria.Op.NNULL);

        AccountDataCenterVirtualSearch = createSearchBuilder();
        AccountDataCenterVirtualSearch.and("account", AccountDataCenterVirtualSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountDataCenterVirtualSearch.and("dc", AccountDataCenterVirtualSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountDataCenterVirtualSearch.join("nicSearch", nicSearch, AccountDataCenterVirtualSearch.entity().getId(), nicSearch.entity().getInstanceId(),
                JoinBuilder.JoinType.INNER);
        AccountDataCenterVirtualSearch.done();

        UserVmByIsoSearch = createSearchBuilder();
        UserVmByIsoSearch.and("isoId", UserVmByIsoSearch.entity().getIsoId(), SearchCriteria.Op.EQ);
        UserVmByIsoSearch.done();

        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }

    @Override
    public List<UserVmVO> listByAccountAndPod(long accountId, long podId) {
        SearchCriteria<UserVmVO> sc = AccountPodSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("pod", podId);

        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<UserVmVO> listByAccountAndDataCenter(long accountId, long dcId) {
        SearchCriteria<UserVmVO> sc = AccountDataCenterSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);

        return listIncludingRemovedBy(sc);
    }

    @Override
    public void updateVM(long id, String displayName, boolean enable, Long osTypeId, String userData, boolean displayVm,
            boolean isDynamicallyScalable, String customId, String hostName, String instanceName) {
        UserVmVO vo = createForUpdate();
        vo.setDisplayName(displayName);
        vo.setHaEnabled(enable);
        vo.setGuestOSId(osTypeId);
        vo.setUserData(userData);
        vo.setDisplayVm(displayVm);
        vo.setDynamicallyScalable(isDynamicallyScalable);
        if (hostName != null) {
            vo.setHostName(hostName);
        }
        if (customId != null) {
            vo.setUuid(customId);
        }
        if(instanceName != null){
            vo.setInstanceName(instanceName);
        }

        update(id, vo);
    }

    @Override
    public List<UserVmVO> findDestroyedVms(Date date) {
        SearchCriteria<UserVmVO> sc = DestroySearch.create();
        sc.setParameters("state", State.Destroyed, State.Expunging, State.Error);
        sc.setParameters("updateTime", date);

        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listByAccountId(long id) {
        SearchCriteria<UserVmVO> sc = AccountSearch.create();
        sc.setParameters("account", id);
        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listByHostId(Long id) {
        SearchCriteria<UserVmVO> sc = HostSearch.create();
        sc.setParameters("host", id);

        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listByIsoId(Long isoId) {
        SearchCriteria<UserVmVO> sc = UserVmByIsoSearch.create();
        sc.setParameters("isoId", isoId);
        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listUpByHostId(Long hostId) {
        SearchCriteria<UserVmVO> sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging});
        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listRunningByHostId(long hostId) {
        SearchCriteria<UserVmVO> sc = HostRunningSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("state", State.Running);

        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listVirtualNetworkInstancesByAcctAndNetwork(long accountId, long networkId) {

        SearchCriteria<UserVmVO> sc = AccountDataCenterVirtualSearch.create();
        sc.setParameters("account", accountId);
        sc.setJoinParameters("nicSearch", "networkId", networkId);

        return listBy(sc);
    }

    /**
     * Recreates UserVmSearch depending on network type, as nics on L2 networks have no ip addresses
     * @param network network
     */
    private void recreateUserVmSeach(NetworkVO network) {
        if (network != null) {
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
            nicSearch.and("removed", nicSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
            if (!Network.GuestType.L2.equals(network.getGuestType())) {
                nicSearch.and().op("ip4Address", nicSearch.entity().getIPv4Address(), SearchCriteria.Op.NNULL);
                nicSearch.or("ip6Address", nicSearch.entity().getIPv6Address(), SearchCriteria.Op.NNULL);
                nicSearch.cp();
            }

            UserVmSearch = createSearchBuilder();
            UserVmSearch.and("states", UserVmSearch.entity().getState(), SearchCriteria.Op.IN);
            UserVmSearch.join("nicSearch", nicSearch, UserVmSearch.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
            UserVmSearch.done();
        }
    }

    @Override
    public List<UserVmVO> listByNetworkIdAndStates(long networkId, State... states) {
        NetworkVO network = networkDao.findById(networkId);
        recreateUserVmSeach(network);

        SearchCriteria<UserVmVO> sc = UserVmSearch.create();
        if (states != null && states.length != 0) {
            sc.setParameters("states", (Object[])states);
        }
        sc.setJoinParameters("nicSearch", "networkId", networkId);

        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listByLastHostId(Long hostId) {
        SearchCriteria<UserVmVO> sc = LastHostSearch.create();
        sc.setParameters("lastHost", hostId);
        sc.setParameters("state", State.Stopped);
        return listBy(sc);
    }

    @Override
    public List<UserVmVO> listByAccountIdAndHostId(long accountId, long hostId) {
        SearchCriteria<UserVmVO> sc = AccountHostSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public void loadDetails(UserVmVO vm) {
        Map<String, String> details = _detailsDao.listDetailsKeyPairs(vm.getId());
        vm.setDetails(details);
    }

    @Override
    public void saveDetails(UserVmVO vm) {
        Map<String, String> detailsStr = vm.getDetails();
        if (detailsStr == null) {
            return;
        }

        final Map<String, Boolean> visibilityMap = _detailsDao.listDetailsVisibility(vm.getId());

        List<UserVmDetailVO> details = new ArrayList<UserVmDetailVO>();
        for (Map.Entry<String, String> entry : detailsStr.entrySet()) {
            boolean display = visibilityMap.getOrDefault(entry.getKey(), true);
            details.add(new UserVmDetailVO(vm.getId(), entry.getKey(), entry.getValue(), display));
        }

        _detailsDao.saveDetails(details);
    }

    @Override
    public List<Long> listPodIdsHavingVmsforAccount(long zoneId, long accountId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<Long> result = new ArrayList<Long>();
        String sql = LIST_PODS_HAVING_VMS_FOR_ACCOUNT;

        try(PreparedStatement pstmt = txn.prepareStatement(sql)) {
            pstmt.setLong(1, zoneId);
            pstmt.setLong(2, accountId);
            try(ResultSet rs = pstmt.executeQuery();)
            {
                while (rs.next()) {
                    result.add(rs.getLong(1));
                }
            }
            catch (Exception e) {
                s_logger.error("listPodIdsHavingVmsforAccount:Exception: " +  e.getMessage());
                throw new CloudRuntimeException("listPodIdsHavingVmsforAccount:Exception: " + e.getMessage(), e);
            }
            txn.commit();
            return result;
        } catch (Exception e) {
            s_logger.error("listPodIdsHavingVmsforAccount:Exception : " +  e.getMessage());
            throw new CloudRuntimeException("listPodIdsHavingVmsforAccount:Exception: " + e.getMessage(), e);
        }
        finally {
            try{
                if (txn != null)
                {
                    txn.close();
                }
            }
            catch (Exception e)
            {
                s_logger.error("listPodIdsHavingVmsforAccount:Exception:" + e.getMessage());
            }
        }

    }

    @Override
    public Hashtable<Long, UserVmData> listVmDetails(Hashtable<Long, UserVmData> userVmDataHash) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            int curr_index = 0;
            List<UserVmData> userVmDataList = new ArrayList(userVmDataHash.values());
            if (userVmDataList.size() > VM_DETAILS_BATCH_SIZE)
            {
                try (PreparedStatement pstmt = txn.prepareStatement(VM_DETAILS + getQueryBatchAppender(VM_DETAILS_BATCH_SIZE));)
                {
                    while ((curr_index + VM_DETAILS_BATCH_SIZE) <= userVmDataList.size()) {
                        // set the vars value
                        for (int k = 1, j = curr_index; j < curr_index + VM_DETAILS_BATCH_SIZE; j++, k++) {
                            pstmt.setLong(k, userVmDataList.get(j).getId());
                        }
                        try(ResultSet rs = pstmt.executeQuery();)
                        {
                            while (rs.next()) {
                                long vm_id = rs.getLong("vm_instance.id");
                                //check if the entry is already there
                                UserVmData uvm = userVmDataHash.get(vm_id);
                                if (uvm == null) {
                                    uvm = new UserVmData();
                                    uvm.setId(vm_id);
                                }
                                // initialize the data with this row
                                setUserVmData(uvm, rs);
                            }
                        }
                        catch (Exception e)
                        {
                            s_logger.error("listVmDetails:Exception:" + e.getMessage());
                            throw new CloudRuntimeException("listVmDetails: Exception:" + e.getMessage(),e);
                        }
                        curr_index += VM_DETAILS_BATCH_SIZE;
                    }
                }
                catch (Exception e)
                {
                    s_logger.error("listVmDetails:Exception:" + e.getMessage());
                    throw new CloudRuntimeException("listVmDetails: Exception:" + e.getMessage(),e);
                }
            }

            if (curr_index < userVmDataList.size()) {
                int batch_size = (userVmDataList.size() - curr_index);
                try (PreparedStatement vm_details_pstmt = txn.prepareStatement(VM_DETAILS + getQueryBatchAppender(batch_size)))
                {
                    // set the vars value
                    for (int k = 1, j = curr_index; j < curr_index + batch_size; j++, k++) {
                        vm_details_pstmt.setLong(k, userVmDataList.get(j).getId());
                    }
                    try(ResultSet rs = vm_details_pstmt.executeQuery();) {
                        while (rs.next()) {
                            long vm_id = rs.getLong("vm_instance.id");
                            //check if the entry is already there
                            UserVmData uvm = userVmDataHash.get(vm_id);
                            if (uvm == null) {
                                uvm = new UserVmData();
                                uvm.setId(vm_id);
                            }
                            // initialize the data with this row
                            setUserVmData(uvm, rs);
                        }
                    }
                    catch (Exception e)
                    {
                        s_logger.error("listVmDetails: Exception:" + e.getMessage());
                        throw new CloudRuntimeException("listVmDetails: Exception:" + e.getMessage(),e);
                    }
                }
                catch (Exception e)
                {
                    s_logger.error("listVmDetails:Exception:" + e.getMessage());
                    throw new CloudRuntimeException("listVmDetails: Exception:" + e.getMessage(),e);
                }
            }
            txn.commit();
            return userVmDataHash;
        } catch (Exception e) {
            s_logger.error("listVmDetails:Exception:" + e.getMessage());
            throw new CloudRuntimeException("listVmDetails:Exception : ", e);
        }
        finally {
            try{
                if (txn != null)
                {
                    txn.close();
                }
            }
            catch (Exception e)
            {
                s_logger.error("listVmDetails:Exception:" + e.getMessage());
            }
        }

    }

    public static UserVmData setUserVmData(UserVmData userVmData, ResultSet rs) throws SQLException {

        if (!userVmData.isInitialized()) {

            //account.account_name, account.type, domain.name,  instance_group.id, instance_group.name,"
            userVmData.setAccountId(rs.getLong("account.id"));
            userVmData.setAccountName(rs.getString("account.account_name"));
            userVmData.setDomainName(rs.getString("domain.name"));

            long grp_id = rs.getLong("instance_group.id");
            if (grp_id > 0) {
                userVmData.setGroupId(grp_id);
                userVmData.setGroup(rs.getString("instance_group.name"));
            }

            //"data_center.id, data_center.name, host.id, host.name, vm_template.id, vm_template.name, vm_template.display_text, vm_template.enable_password,
            userVmData.setZoneId(rs.getLong("data_center.id"));
            userVmData.setZoneName(rs.getString("data_center.name"));

            userVmData.setHostId(rs.getLong("host.id"));
            userVmData.setHostName(rs.getString("host.name"));

            long template_id = rs.getLong("vm_template.id");
            if (template_id > 0) {
                userVmData.setTemplateId(template_id);
                userVmData.setTemplateName(rs.getString("vm_template.name"));
                userVmData.setTemplateDisplayText(rs.getString("vm_template.display_text"));
                userVmData.setPasswordEnabled(rs.getBoolean("vm_template.enable_password"));
            } else {
                userVmData.setTemplateId(-1L);
                userVmData.setTemplateName("ISO Boot");
                userVmData.setTemplateDisplayText("ISO Boot");
                userVmData.setPasswordEnabled(false);
            }

            long iso_id = rs.getLong("iso.id");
            if (iso_id > 0) {
                userVmData.setIsoId(iso_id);
                userVmData.setIsoName(rs.getString("iso.name"));
            }

            //service_offering.id, disk_offering.name, "
            //"service_offering.cpu, service_offering.speed, service_offering.ram_size,
            userVmData.setServiceOfferingId(rs.getLong("service_offering.id"));
            userVmData.setServiceOfferingName(rs.getString("disk_offering.name"));
            userVmData.setCpuNumber(rs.getInt("service_offering.cpu"));
            userVmData.setCpuSpeed(rs.getInt("service_offering.speed"));
            userVmData.setMemory(rs.getInt("service_offering.ram_size"));

            // volumes.device_id, volumes.volume_type,
            long vol_id = rs.getLong("volumes.id");
            if (vol_id > 0) {
                userVmData.setRootDeviceId(rs.getLong("volumes.device_id"));
                userVmData.setRootDeviceType(rs.getString("volumes.volume_type"));
                // storage pool
                long pool_id = rs.getLong("storage_pool.id");
                if (pool_id > 0) {
                    userVmData.setRootDeviceType(rs.getString("storage_pool.pool_type"));
                } else {
                    userVmData.setRootDeviceType("Not created");
                }
            }
            userVmData.setInitialized();
        }

        Long securityGroupId = rs.getLong("security_group.id");
        if (securityGroupId != null && securityGroupId.longValue() != 0) {
            SecurityGroupData resp = userVmData.newSecurityGroupData();
            resp.setId(rs.getLong("security_group.id"));
            resp.setName(rs.getString("security_group.name"));
            resp.setDescription(rs.getString("security_group.description"));
            resp.setObjectName("securitygroup");
            userVmData.addSecurityGroup(resp);
        }

        long nic_id = rs.getLong("nics.id");
        if (nic_id > 0) {
            NicData nicResponse = userVmData.newNicData();
            nicResponse.setId(nic_id);
            nicResponse.setIpaddress(rs.getString("nics.ip4_address"));
            nicResponse.setGateway(rs.getString("nics.gateway"));
            nicResponse.setNetmask(rs.getString("nics.netmask"));
            nicResponse.setNetworkid(rs.getLong("nics.network_id"));
            nicResponse.setMacAddress(rs.getString("nics.mac_address"));

            int account_type = rs.getInt("account.type");
            if (account_type == Account.ACCOUNT_TYPE_ADMIN) {
                nicResponse.setBroadcastUri(rs.getString("nics.broadcast_uri"));
                nicResponse.setIsolationUri(rs.getString("nics.isolation_uri"));
            }

            nicResponse.setTrafficType(rs.getString("networks.traffic_type"));
            nicResponse.setType(rs.getString("networks.guest_type"));
            nicResponse.setIsDefault(rs.getBoolean("nics.default_nic"));
            nicResponse.setMtu(rs.getInt("nics.mtu"));
            nicResponse.setObjectName("nic");
            userVmData.addNic(nicResponse);
        }

        long publicIpId = rs.getLong("user_ip_address.id");
        if (publicIpId > 0) {
            userVmData.setPublicIpId(publicIpId);
            userVmData.setPublicIp(rs.getString("user_ip_address.public_ip_address"));
        }

        return userVmData;
    }

    public String getQueryBatchAppender(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(" ?,");
        }
        sb.deleteCharAt(sb.length() - 1).append(")");
        return sb.toString();
    }

    @Override
    public Long countAllocatedVMsForAccount(long accountId) {
        SearchCriteria<Long> sc = CountByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("type", VirtualMachine.Type.User);
        sc.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging});
        sc.setParameters("displayVm", 1);
        return customSearch(sc, null).get(0);
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        _tagsDao.removeByIdAndType(id, ResourceObjectType.UserVm);
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>>> getVmsDetailByNames(Set<String> vmNames, String detail) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>>> vmsDetailByNames = new ArrayList<Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>>>();

        try (PreparedStatement pstmt = txn.prepareStatement(VMS_DETAIL_BY_NAME + getQueryBatchAppender(vmNames.size()));) {
            pstmt.setString(1, detail);
            int i = 2;
            for(String name : vmNames) {
                pstmt.setString(i, name);
                i++;
            }
            try (ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    vmsDetailByNames.add(new Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>>(new Pair<String, VirtualMachine.Type>(
                            rs.getString("vm_instance.instance_name"), VirtualMachine.Type.valueOf(rs.getString("vm_type"))),
                            new Pair<Long, String>(rs.getLong("vm_instance.id"), rs.getString("user_vm_details.value"))));
                }
            }
        } catch (SQLException e) {
            s_logger.error("GetVmsDetailsByNames: Exception in sql: " + e.getMessage());
            throw new CloudRuntimeException("GetVmsDetailsByNames: Exception: " + e.getMessage());
        }

        return vmsDetailByNames;
    }
}
