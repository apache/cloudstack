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
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SequenceFetcher;
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

    protected static final DataCenterIpAddressDaoImpl _ipAllocDao = ComponentLocator.inject(DataCenterIpAddressDaoImpl.class);
    protected static final DataCenterLinkLocalIpAddressDaoImpl _LinkLocalIpAllocDao = ComponentLocator.inject(DataCenterLinkLocalIpAddressDaoImpl.class);
    protected static final DataCenterVnetDaoImpl _vnetAllocDao = ComponentLocator.inject(DataCenterVnetDaoImpl.class);
    protected static final PodVlanDaoImpl _podVlanAllocDao = ComponentLocator.inject(PodVlanDaoImpl.class);
    protected long _prefix;
    protected Random _rand = new Random(System.currentTimeMillis());
    protected TableGenerator _tgMacAddress;

    @Override
    public DataCenterVO findByName(String name) {
    	SearchCriteria sc = NameSearch.create();
    	sc.setParameters("name", name);
        return findOneActiveBy(sc);
    }

    @Override
    public void releaseVnet(String vnet, long dcId, long accountId) {
        _vnetAllocDao.release(vnet, dcId, accountId);
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
    public void releaseLinkLocalPrivateIpAddress(String ipAddress, long dcId, Long instanceId) {
    	_LinkLocalIpAllocDao.releaseIpAddress(ipAddress, dcId, instanceId);
    }
    
    @Override
    public boolean deletePrivateIpAddressByPod(long podId) {
    	return _ipAllocDao.deleteIpAddressByPod(podId);
    }
    
    @Override
    public boolean deleteLinkLocalPrivateIpAddressByPod(long podId) {
    	return _LinkLocalIpAllocDao.deleteIpAddressByPod(podId);
    }

    @Override
    public String allocateVnet(long dataCenterId, long accountId) {
        DataCenterVnetVO vo = _vnetAllocDao.take(dataCenterId, accountId);
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
    public String allocatePrivateIpAddress(long dcId, long podId, long instanceId) {
        DataCenterIpAddressVO vo = _ipAllocDao.takeIpAddress(dcId, podId, instanceId);
        if (vo == null) {
            return null;
        }
        return vo.getIpAddress();
    }
    
    @Override
    public String allocateLinkLocalPrivateIpAddress(long dcId, long podId, long instanceId) {
    	DataCenterLinkLocalIpAddressVO vo = _LinkLocalIpAllocDao.takeIpAddress(dcId, podId, instanceId);
        if (vo == null) {
            return null;
        }
        return vo.getIpAddress();
    }
    
    @Override
    public void addVnet(long dcId, int start, int end) {
        _vnetAllocDao.add(dcId, start, end);
    }
    
    @Override
    public void deleteVnet(long dcId) {
    	_vnetAllocDao.delete(dcId);
    }
    
    @Override
    public List<DataCenterVnetVO> listAllocatedVnets(long dcId) {
    	return _vnetAllocDao.listAllocatedVnets(dcId);
    }
    
    @Override
    public void addPrivateIpAddress(long dcId,long podId, String start, String end) {
        _ipAllocDao.addIpRange(dcId, podId, start, end);
    }
    
    @Override
    public void addLinkLocalPrivateIpAddress(long dcId,long podId, String start, String end) {
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
        
        _tgMacAddress = _tgs.get("macAddress");
        assert _tgMacAddress != null : "Couldn't get mac address table generator";
    }
}
