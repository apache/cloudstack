/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.vm.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.response.NicResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

@Local(value={UserVmDao.class})
public class UserVmDaoImpl extends GenericDaoBase<UserVmVO, Long> implements UserVmDao {
    public static final Logger s_logger = Logger.getLogger(UserVmDaoImpl.class);
    
    protected final SearchBuilder<UserVmVO> AccountPodSearch;
    protected final SearchBuilder<UserVmVO> AccountDataCenterSearch;
    protected final SearchBuilder<UserVmVO> AccountSearch;
    protected final SearchBuilder<UserVmVO> HostSearch;
    protected final SearchBuilder<UserVmVO> LastHostSearch;
    protected final SearchBuilder<UserVmVO> HostUpSearch;
    protected final SearchBuilder<UserVmVO> HostRunningSearch;
    protected final SearchBuilder<UserVmVO> StateChangeSearch;
    protected final SearchBuilder<UserVmVO> AccountHostSearch;

    protected final SearchBuilder<UserVmVO> DestroySearch;
    protected SearchBuilder<UserVmVO> AccountDataCenterVirtualSearch;
    protected GenericSearchBuilder<UserVmVO, Long> CountByAccountPod;
    protected GenericSearchBuilder<UserVmVO, Long> CountByAccount;
    protected GenericSearchBuilder<UserVmVO, Long> PodsHavingVmsForAccount;
    
    protected SearchBuilder<UserVmVO> UserVmSearch;
    protected final Attribute _updateTimeAttr;
   
    private static final String LIST_PODS_HAVING_VMS_FOR_ACCOUNT = "SELECT pod_id FROM cloud.vm_instance WHERE data_center_id = ? AND account_id = ? AND pod_id IS NOT NULL AND state = 'Running' OR state = 'Stopped' " +
    		"GROUP BY pod_id HAVING count(id) > 0 ORDER BY count(id) DESC";

    private static final String VM_DETAILS = "select account.account_name, account.type, domain.name, instance_group.id, instance_group.name," +
    		"data_center.id, data_center.name, data_center.is_security_group_enabled, host.id, host.name, " + 
    		"vm_template.id, vm_template.name, vm_template.display_text, iso.id, iso.name, " +
    		"vm_template.enable_password, service_offering.id, disk_offering.name, storage_pool.id, storage_pool.pool_type, " +
    		"service_offering.cpu, service_offering.speed, service_offering.ram_size, volumes.id, volumes.device_id, volumes.volume_type, security_group.id, security_group.name, " +
    		"security_group.description, nics.id, nics.ip4_address, nics.gateway, nics.network_id, nics.netmask, nics.mac_address, nics.broadcast_uri, nics.isolation_uri, " +
    		"networks.traffic_type, networks.guest_type, networks.is_default from vm_instance " +
            "left join account on vm_instance.account_id=account.id  " +
            "left join domain on vm_instance.domain_id=domain.id " +
            "left join instance_group_vm_map on vm_instance.id=instance_group_vm_map.instance_id " +
            "left join instance_group on instance_group_vm_map.group_id=instance_group.id " + 
            "left join data_center on vm_instance.data_center_id=data_center.id " +
            "left join host on vm_instance.host_id=host.id " + 
            "left join vm_template on vm_instance.vm_template_id=vm_template.id " +
            "left join vm_template iso on iso.id=? " + 
            "left join service_offering on vm_instance.service_offering_id=service_offering.id " +
            "left join disk_offering  on vm_instance.service_offering_id=disk_offering.id " +
            "left join volumes on vm_instance.id=volumes.instance_id " +
            "left join storage_pool on volumes.pool_id=storage_pool.id " +
            "left join security_group_vm_map on vm_instance.id=security_group_vm_map.instance_id " +
            "left join security_group on security_group_vm_map.security_group_id=security_group.id " +
            "left join nics on vm_instance.id=nics.instance_id " +
            "left join networks on nics.network_id=networks.id " +
            "where vm_instance.id=?";
    
    protected final UserVmDetailsDaoImpl _detailsDao = ComponentLocator.inject(UserVmDetailsDaoImpl.class);
    
    protected UserVmDaoImpl() {
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
        AccountDataCenterSearch.and("dc", AccountDataCenterSearch.entity().getDataCenterIdToDeployIn(), SearchCriteria.Op.EQ);
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
        CountByAccount.done();

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
    public void updateVM(long id, String displayName, boolean enable, Long osTypeId, String userData) {
        UserVmVO vo = createForUpdate();
        vo.setDisplayName(displayName);
        vo.setHaEnabled(enable);
        vo.setGuestOSId(osTypeId);
        vo.setUserData(userData);
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
    public List<UserVmVO> listVirtualNetworkInstancesByAcctAndZone(long accountId, long dcId, long networkId) {
        if (AccountDataCenterVirtualSearch == null) {
            NicDao _nicDao = ComponentLocator.getLocator("management-server").getDao(NicDao.class);
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
            nicSearch.and("ip4Address", nicSearch.entity().getIp4Address(), SearchCriteria.Op.NNULL);

            AccountDataCenterVirtualSearch = createSearchBuilder();
            AccountDataCenterVirtualSearch.and("account", AccountDataCenterVirtualSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
            AccountDataCenterVirtualSearch.and("dc", AccountDataCenterVirtualSearch.entity().getDataCenterIdToDeployIn(), SearchCriteria.Op.EQ);
            AccountDataCenterVirtualSearch.join("nicSearch", nicSearch, AccountDataCenterVirtualSearch.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
            AccountDataCenterVirtualSearch.done();
        }

        SearchCriteria<UserVmVO> sc = AccountDataCenterVirtualSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setJoinParameters("nicSearch", "networkId", networkId);

        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listByNetworkIdAndStates(long networkId, State... states) {
        if (UserVmSearch == null) {
            NicDao _nicDao = ComponentLocator.getLocator("management-server").getDao(NicDao.class);
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
            nicSearch.and("ip4Address", nicSearch.entity().getIp4Address(), SearchCriteria.Op.NNULL);

            UserVmSearch = createSearchBuilder();
            UserVmSearch.and("states", UserVmSearch.entity().getState(), SearchCriteria.Op.IN);
            UserVmSearch.join("nicSearch", nicSearch, UserVmSearch.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
            UserVmSearch.done();
        }

        SearchCriteria<UserVmVO> sc = UserVmSearch.create();
        if (states != null && states.length != 0) {
            sc.setParameters("states", (Object[]) states);
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
        Map<String, String> details = _detailsDao.findDetails(vm.getId());
        vm.setDetails(details);
	}
	
	@Override
    public void saveDetails(UserVmVO vm) {
        Map<String, String> details = vm.getDetails();
        if (details == null) {
            return;
        }
        _detailsDao.persist(vm.getId(), details);
    }
	
    @Override
    public List<Long> listPodIdsHavingVmsforAccount(long zoneId, long accountId){
    	Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        try {
            String sql = LIST_PODS_HAVING_VMS_FOR_ACCOUNT;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, zoneId);
            pstmt.setLong(2, accountId);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + LIST_PODS_HAVING_VMS_FOR_ACCOUNT, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + LIST_PODS_HAVING_VMS_FOR_ACCOUNT, e);
        }
    }
    
    @Override
    public UserVmResponse listVmDetails(UserVm userVm, boolean show_host){
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;

        try {
            String sql = VM_DETAILS;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, userVm.getIsoId() == null ? -1 : userVm.getIsoId());
            pstmt.setLong(2, userVm.getId());
            
            ResultSet rs = pstmt.executeQuery();
            boolean is_data_center_security_group_enabled=false;
            Set<SecurityGroupResponse> securityGroupResponse = new HashSet<SecurityGroupResponse>();
            Set<NicResponse> nicResponses = new HashSet<NicResponse>();
            UserVmResponse userVmResponse = null;
            while (rs.next()) {
                if (userVmResponse==null){
                    userVmResponse=new UserVmResponse();
                    userVmResponse.setId(userVm.getId());
                    userVmResponse.setName(userVm.getInstanceName());
                    userVmResponse.setCreated(userVm.getCreated());
                    userVmResponse.setGuestOsId(userVm.getGuestOSId());
                    userVmResponse.setHaEnable(userVm.isHaEnabled());
                    if (userVm.getState() != null) {
                        userVmResponse.setState(userVm.getState().toString());
                    }
                    if (userVm.getDisplayName() != null) {
                        userVmResponse.setDisplayName(userVm.getDisplayName());
                    } else {
                        userVmResponse.setDisplayName(userVm.getHostName());
                    }
                    
                    //account.account_name, account.type, domain.name,  instance_group.id, instance_group.name,"
                    
                    userVmResponse.setAccountName(rs.getString("account.account_name"));
                    userVmResponse.setDomainId(userVm.getDomainId());
                    userVmResponse.setDomainName(rs.getString("domain.name"));
                    
                    long grp_id = rs.getLong("instance_group.id");
                    if (grp_id > 0){
                        userVmResponse.setGroupId(grp_id);
                        userVmResponse.setGroup(rs.getString("instance_group.name"));
                    }
                    
                    //"data_center.id, data_center.name, host.id, host.name, vm_template.id, vm_template.name, vm_template.display_text, vm_template.enable_password, 
                    userVmResponse.setZoneId(rs.getLong("data_center.id"));
                    userVmResponse.setZoneName(rs.getString("data_center.name"));
                    
                    if (show_host){
                        userVmResponse.setHostId(rs.getLong("host.id"));
                        userVmResponse.setHostName(rs.getString("host.name"));
                    }

                    if (userVm.getHypervisorType() != null) {
                        userVmResponse.setHypervisor(userVm.getHypervisorType().toString());
                    }
                    
                    long template_id = rs.getLong("vm_template.id");
                    if (template_id > 0){
                        userVmResponse.setTemplateId(template_id);
                        userVmResponse.setTemplateName(rs.getString("vm_template.name"));
                        userVmResponse.setTemplateDisplayText(rs.getString("vm_template.display_text"));
                        userVmResponse.setPasswordEnabled(rs.getBoolean("vm_template.enable_password"));
                    }
                    else {
                        userVmResponse.setTemplateId(-1L);
                        userVmResponse.setTemplateName("ISO Boot");
                        userVmResponse.setTemplateDisplayText("ISO Boot");
                        userVmResponse.setPasswordEnabled(false);
                    }
                    
                    long iso_id = rs.getLong("iso.id");
                    if (iso_id > 0){
                        userVmResponse.setIsoId(iso_id);
                        userVmResponse.setIsoName(rs.getString("iso.name"));
                    }

                    if (userVm.getPassword() != null) {
                        userVmResponse.setPassword(userVm.getPassword());
                    }
    
                    //service_offering.id, disk_offering.name, " 
                    //"service_offering.cpu, service_offering.speed, service_offering.ram_size,
                    userVmResponse.setServiceOfferingId(rs.getLong("service_offering.id"));
                    userVmResponse.setServiceOfferingName(rs.getString("disk_offering.name"));
                    userVmResponse.setCpuNumber(rs.getInt("service_offering.cpu"));
                    userVmResponse.setCpuSpeed(rs.getInt("service_offering.speed"));
                    userVmResponse.setMemory(rs.getInt("service_offering.ram_size"));

                    // volumes.device_id, volumes.volume_type, 
                    long vol_id = rs.getLong("volumes.id");
                    if (vol_id > 0){
                        userVmResponse.setRootDeviceId(rs.getLong("volumes.device_id"));
                        userVmResponse.setRootDeviceType(rs.getString("volumes.volume_type"));
                        // storage pool
                        long pool_id = rs.getLong("storage_pool.id");
                        if (pool_id > 0){
                            userVmResponse.setRootDeviceType(rs.getString("storage_pool.pool_type"));
                        }
                        else {
                            userVmResponse.setRootDeviceType("Not created");
                        }
                    }
                    is_data_center_security_group_enabled = rs.getBoolean("data_center.is_security_group_enabled");
                }
                
                //security_group.id, security_group.name, security_group.description, , data_center.is_security_group_enabled
                if (is_data_center_security_group_enabled){
                    SecurityGroupResponse resp = new SecurityGroupResponse();
                    resp.setId(rs.getLong("security_group.id"));
                    resp.setName(rs.getString("security_group.name"));
                    resp.setDescription(rs.getString("security_group.description"));
                    resp.setObjectName("securitygroup");
                    securityGroupResponse.add(resp);
                }
                
                
                //nics.id, nics.ip4_address, nics.gateway, nics.network_id, nics.netmask, nics. mac_address, nics.broadcast_uri, nics.isolation_uri, " +
                //"networks.traffic_type, networks.guest_type, networks.is_default from vm_instance, "
                long nic_id = rs.getLong("nics.id");
                if (nic_id > 0){
                    NicResponse nicResponse = new NicResponse();
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
                    nicResponse.setIsDefault(rs.getBoolean("networks.is_default"));
                    nicResponse.setObjectName("nic");
                    nicResponses.add(nicResponse);
                }
                
            }
            userVmResponse.setSecurityGroupList(new ArrayList(securityGroupResponse));
            userVmResponse.setNics(new ArrayList(nicResponses));
            return userVmResponse;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + VM_DETAILS, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + VM_DETAILS, e);
        }
    }

    @Override
    public Long countAllocatedVMsForAccount(long accountId) {
    	SearchCriteria<Long> sc = CountByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("type", VirtualMachine.Type.User);
		sc.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging});
        return customSearch(sc, null).get(0);
    }
}
