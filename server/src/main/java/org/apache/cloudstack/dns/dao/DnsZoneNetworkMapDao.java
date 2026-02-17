package org.apache.cloudstack.dns.dao;

import java.util.List;

import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;

import com.cloud.utils.db.GenericDao;

public interface DnsZoneNetworkMapDao extends GenericDao<DnsZoneNetworkMapVO, Long> {
    List<DnsZoneNetworkMapVO> listByDnsZoneId(long dnsZoneId);
    DnsZoneNetworkMapVO findByZoneAndNetwork(long dnsZoneId, long networkId);
    List<DnsZoneNetworkMapVO> listByNetworkId(long networkId);
}
