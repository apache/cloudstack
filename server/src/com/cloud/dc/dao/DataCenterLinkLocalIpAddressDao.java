package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.utils.db.GenericDao;

public interface DataCenterLinkLocalIpAddressDao extends GenericDao<DataCenterLinkLocalIpAddressVO, Long>{
    public DataCenterLinkLocalIpAddressVO takeIpAddress(long dcId, long podId, long instanceId, String reservationId);
    public boolean deleteIpAddressByPod(long podId);
    public void addIpRange(long dcId, long podId, String start, String end);
    public void releaseIpAddress(String ipAddress, long dcId, long instanceId);
    public void releaseIpAddress(long nicId, String reservationId);
    public List<DataCenterLinkLocalIpAddressVO> listByPodIdDcId(long podId, long dcId);
    public int countIPs(long podId, long dcId, boolean onlyCountAllocated);
}
