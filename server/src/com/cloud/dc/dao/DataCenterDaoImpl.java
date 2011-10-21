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

package com.cloud.dc.dao;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.persistence.TableGenerator;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenterVnetVO;
import com.cloud.dc.PodVlanVO;
import com.cloud.org.Grouping;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SequenceFetcher;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;

/**
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || mac.address.prefix | prefix to attach to all public and private mac addresses | number | 06 ||
 *  }
 **/
@Local(value={DataCenterDao.class})
public class DataCenterDaoImpl extends GenericDaoBase<DataCenterVO, Long> implements DataCenterDao {
    private static final Logger s_logger = Logger.getLogger(DataCenterDaoImpl.class);

    protected SearchBuilder<DataCenterVO> NameSearch;
    protected SearchBuilder<DataCenterVO> ListZonesByDomainIdSearch;
    protected SearchBuilder<DataCenterVO> PublicZonesSearch;
    protected SearchBuilder<DataCenterVO> ChildZonesSearch;
    protected SearchBuilder<DataCenterVO> DisabledZonesSearch;
    protected SearchBuilder<DataCenterVO> TokenSearch;
    
    protected final DataCenterIpAddressDaoImpl _ipAllocDao = ComponentLocator.inject(DataCenterIpAddressDaoImpl.class);
    protected final DataCenterLinkLocalIpAddressDaoImpl _LinkLocalIpAllocDao = ComponentLocator.inject(DataCenterLinkLocalIpAddressDaoImpl.class);
    protected final DataCenterVnetDaoImpl _vnetAllocDao = ComponentLocator.inject(DataCenterVnetDaoImpl.class);
    protected final PodVlanDaoImpl _podVlanAllocDao = ComponentLocator.inject(PodVlanDaoImpl.class);
    protected long _prefix;
    protected Random _rand = new Random(System.currentTimeMillis());
    protected TableGenerator _tgMacAddress;
    
    protected final DcDetailsDaoImpl _detailsDao = ComponentLocator.inject(DcDetailsDaoImpl.class);


    @Override
    public DataCenterVO findByName(String name) {
    	SearchCriteria<DataCenterVO> sc = NameSearch.create();
    	sc.setParameters("name", name);
        return findOneBy(sc);
    }
    
    @Override
    public DataCenterVO findByToken(String zoneToken){
    	SearchCriteria<DataCenterVO> sc = TokenSearch.create();
    	sc.setParameters("zoneToken", zoneToken);
        return findOneBy(sc);
    }
    
    @Override
    public List<DataCenterVO> findZonesByDomainId(Long domainId){
    	SearchCriteria<DataCenterVO> sc = ListZonesByDomainIdSearch.create();
    	sc.setParameters("domainId", domainId);
        return listBy(sc);    	
    }
    
    @Override
    public List<DataCenterVO> findChildZones(Object[] ids){
    	SearchCriteria<DataCenterVO> sc = ChildZonesSearch.create();
    	sc.setParameters("domainid", ids);
        return listBy(sc);  
    }
    
    @Override
    public List<DataCenterVO> listPublicZones(){
    	SearchCriteria<DataCenterVO> sc = PublicZonesSearch.create();
    	//sc.setParameters("domainId", domainId);
        return listBy(sc);    	    	
    }
    
    @Override
    public void releaseVnet(String vnet, long dcId, long physicalNetworkId, long accountId, String reservationId) {
        _vnetAllocDao.release(vnet, physicalNetworkId, accountId, reservationId);
    }
    
    @Override
    public List<DataCenterVnetVO> findVnet(long dcId, String vnet) {
    	return _vnetAllocDao.findVnet(dcId, vnet);
    }

    @Override
    public void releasePrivateIpAddress(String ipAddress, long dcId, Long instanceId) {
        _ipAllocDao.releaseIpAddress(ipAddress, dcId, instanceId);
    }
    
    @Override
    public void releasePrivateIpAddress(long nicId, String reservationId) {
        _ipAllocDao.releaseIpAddress(nicId, reservationId);
    }
    
    @Override
    public void releaseLinkLocalIpAddress(long nicId, String reservationId) {
        _LinkLocalIpAllocDao.releaseIpAddress(nicId, reservationId);
    }
    
    @Override
    public void releaseLinkLocalIpAddress(String ipAddress, long dcId, Long instanceId) {
    	_LinkLocalIpAllocDao.releaseIpAddress(ipAddress, dcId, instanceId);
    }
    
    @Override
    public boolean deletePrivateIpAddressByPod(long podId) {
    	return _ipAllocDao.deleteIpAddressByPod(podId);
    }
    
    @Override
    public boolean deleteLinkLocalIpAddressByPod(long podId) {
    	return _LinkLocalIpAllocDao.deleteIpAddressByPod(podId);
    }

    @Override
    public String allocateVnet(long dataCenterId, long physicalNetworkId, long accountId, String reservationId) {
        DataCenterVnetVO vo = _vnetAllocDao.take(physicalNetworkId, accountId, reservationId);
        if (vo == null) {
            return null;
        }

        return vo.getVnet();
    }
    
    @Override
    public String allocatePodVlan(long podId, long accountId) {
        PodVlanVO vo = _podVlanAllocDao.take(podId, accountId);
        if (vo == null) {
            return null;
        }
        return vo.getVlan();
    }

    @Override
    public String[] getNextAvailableMacAddressPair(long id) {
        return getNextAvailableMacAddressPair(id, 0);
    }

    @Override
    public String[] getNextAvailableMacAddressPair(long id, long mask) {
        SequenceFetcher fetch = SequenceFetcher.getInstance();
        
        long seq = fetch.getNextSequence(Long.class, _tgMacAddress, id);
        seq = seq | _prefix | ((id & 0x7f) << 32);
        seq |= mask;
        seq |= ((_rand.nextInt(Short.MAX_VALUE) << 16) & 0x00000000ffff0000l);
        String[] pair = new String[2];
        pair[0] = NetUtils.long2Mac(seq);
        pair[1] = NetUtils.long2Mac(seq | 0x1l << 39);
        return pair;
    }

    @Override
    public Pair<String, Long> allocatePrivateIpAddress(long dcId, long podId, long instanceId, String reservationId) {
        DataCenterIpAddressVO vo = _ipAllocDao.takeIpAddress(dcId, podId, instanceId, reservationId);
        if (vo == null) {
            return null;
        }
        return new Pair<String, Long>(vo.getIpAddress(), vo.getMacAddress());
    }
    
    @Override
    public String allocateLinkLocalIpAddress(long dcId, long podId, long instanceId, String reservationId) {
    	DataCenterLinkLocalIpAddressVO vo = _LinkLocalIpAllocDao.takeIpAddress(dcId, podId, instanceId, reservationId);
        if (vo == null) {
            return null;
        }
        return vo.getIpAddress();
    }
   
    @Override
    public void addVnet(long dcId, long physicalNetworkId, int start, int end) {
        _vnetAllocDao.add(dcId, physicalNetworkId, start, end);
    }
    
    @Override
    public void deleteVnet(long physicalNetworkId) {
        _vnetAllocDao.delete(physicalNetworkId);
    }
    
    @Override
    public List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId) {
        return _vnetAllocDao.listAllocatedVnets(physicalNetworkId);
    }    
    
    @Override
    public void addPrivateIpAddress(long dcId,long podId, String start, String end) {
        _ipAllocDao.addIpRange(dcId, podId, start, end);
    }
    
    @Override
    public void addLinkLocalIpAddress(long dcId,long podId, String start, String end) {
    	_LinkLocalIpAllocDao.addIpRange(dcId, podId, start, end);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            return false;
        }
        
        String value = (String)params.get("mac.address.prefix");
        _prefix = (long)NumbersUtil.parseInt(value, 06) << 40;

        if (!_ipAllocDao.configure("Ip Alloc", params)) {
            return false;
        }

        if (!_vnetAllocDao.configure("vnet Alloc", params)) {
            return false;
        }
        return true;
    }
    
    protected DataCenterDaoImpl() {
        super();
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();
        
        ListZonesByDomainIdSearch = createSearchBuilder();
        ListZonesByDomainIdSearch.and("domainId", ListZonesByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListZonesByDomainIdSearch.done();
        
        PublicZonesSearch = createSearchBuilder();
        PublicZonesSearch.and("domainId", PublicZonesSearch.entity().getDomainId(), SearchCriteria.Op.NULL);
        PublicZonesSearch.done();        
        
        ChildZonesSearch = createSearchBuilder();
        ChildZonesSearch.and("domainid", ChildZonesSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        ChildZonesSearch.done();
        
        DisabledZonesSearch = createSearchBuilder();
        DisabledZonesSearch.and("allocationState", DisabledZonesSearch.entity().getAllocationState(), SearchCriteria.Op.EQ);
        DisabledZonesSearch.done();
        
        TokenSearch = createSearchBuilder();
        TokenSearch.and("zoneToken", TokenSearch.entity().getZoneToken(), SearchCriteria.Op.EQ);
        TokenSearch.done();                
        
        _tgMacAddress = _tgs.get("macAddress");
        assert _tgMacAddress != null : "Couldn't get mac address table generator";
    }

    @Override @DB
    public boolean update(Long zoneId, DataCenterVO zone) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        boolean persisted = super.update(zoneId, zone);
        if (!persisted) {
            return persisted;
        }
        saveDetails(zone);
        txn.commit();
        return persisted;
    }
    
    @Override
    public void loadDetails(DataCenterVO zone) {
        Map<String, String> details =_detailsDao.findDetails(zone.getId());
        zone.setDetails(details);
    }

    @Override
    public void saveDetails(DataCenterVO zone) {
        Map<String, String> details = zone.getDetails();
        if (details == null) {
            return;
        }
        _detailsDao.persist(zone.getId(), details);
    }
    
    @Override
    public List<DataCenterVO> listDisabledZones(){
    	SearchCriteria<DataCenterVO> sc = DisabledZonesSearch.create();
    	sc.setParameters("allocationState", Grouping.AllocationState.Disabled);
    	
    	List<DataCenterVO> dcs =  listBy(sc);
    	
    	return dcs;
    }
    
    @Override
    public List<DataCenterVO> listEnabledZones(){
    	SearchCriteria<DataCenterVO> sc = DisabledZonesSearch.create();
    	sc.setParameters("allocationState", Grouping.AllocationState.Enabled);
    	
    	List<DataCenterVO> dcs =  listBy(sc);
    	
    	return dcs;
    }

    @Override
    public DataCenterVO findByTokenOrIdOrName(String tokenOrIdOrName) {
        DataCenterVO result = findByToken(tokenOrIdOrName);
        if (result == null) {
            result = findByName(tokenOrIdOrName);
            if (result == null) {
                try {
                    Long dcId = Long.parseLong(tokenOrIdOrName);
                    return findById(dcId);
                } catch (NumberFormatException nfe) {
                    
                }
            }
        }
        return result;
    }
    
    @Override
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        DataCenterVO zone = createForUpdate();
        zone.setName(null);
        
        update(id, zone);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }
}
