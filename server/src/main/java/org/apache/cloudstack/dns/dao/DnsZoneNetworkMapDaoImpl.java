package org.apache.cloudstack.dns.dao;

import java.util.List;

import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsZoneNetworkMapDaoImpl extends GenericDaoBase<DnsZoneNetworkMapVO, Long> implements DnsZoneNetworkMapDao {
    final SearchBuilder<DnsZoneNetworkMapVO> ZoneSearch;

    public DnsZoneNetworkMapDaoImpl() {
        super();
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("dnsZoneId", ZoneSearch.entity().getDnsZoneId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();
    }
    @Override
    public List<DnsZoneNetworkMapVO> listByDnsZoneId(long dnsZoneId) {
        SearchCriteria<DnsZoneNetworkMapVO> sc = ZoneSearch.create();
        sc.setParameters("dnsZoneId", dnsZoneId);
        return listBy(sc);
    }

    @Override
    public DnsZoneNetworkMapVO findByZoneAndNetwork(long zoneId, long networkId) {
        return null;
    }

    @Override
    public List<DnsZoneNetworkMapVO> listByNetworkId(long networkId) {
        return List.of();
    }
}
