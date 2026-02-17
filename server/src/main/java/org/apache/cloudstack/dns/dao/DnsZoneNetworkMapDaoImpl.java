package org.apache.cloudstack.dns.dao;

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsZoneNetworkMapDaoImpl extends GenericDaoBase<DnsZoneNetworkMapVO, Long> implements DnsZoneNetworkMapDao {
    private final SearchBuilder<DnsZoneNetworkMapVO> ZoneNetworkSearch;
    private final SearchBuilder<DnsZoneNetworkMapVO> ZoneSearch;
    private final SearchBuilder<DnsZoneNetworkMapVO> NetworkSearch;

    public DnsZoneNetworkMapDaoImpl() {
        super();
        ZoneNetworkSearch = createSearchBuilder();
        ZoneNetworkSearch.and(ApiConstants.DNS_ZONE_ID, ZoneNetworkSearch.entity().getDnsZoneId(), SearchCriteria.Op.EQ);
        ZoneNetworkSearch.and(ApiConstants.NETWORK_ID, ZoneNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        ZoneNetworkSearch.done();

        ZoneSearch = createSearchBuilder();
        ZoneSearch.and(ApiConstants.DNS_ZONE_ID, ZoneSearch.entity().getDnsZoneId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();

        NetworkSearch = createSearchBuilder();
        NetworkSearch.and(ApiConstants.NETWORK_ID, NetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkSearch.done();
    }
    @Override
    public List<DnsZoneNetworkMapVO> listByDnsZoneId(long dnsZoneId) {
        SearchCriteria<DnsZoneNetworkMapVO> sc = ZoneSearch.create();
        sc.setParameters(ApiConstants.DNS_ZONE_ID, dnsZoneId);
        return listBy(sc);
    }

    @Override
    public DnsZoneNetworkMapVO findByZoneAndNetwork(long dnsZoneId, long networkId) {
        SearchCriteria<DnsZoneNetworkMapVO> sc = ZoneNetworkSearch.create();
        sc.setParameters(ApiConstants.DNS_ZONE_ID, dnsZoneId);
        sc.setParameters(ApiConstants.NETWORK_ID, networkId);

        return findOneBy(sc);
    }

    @Override
    public List<DnsZoneNetworkMapVO> listByNetworkId(long networkId) {
        SearchCriteria<DnsZoneNetworkMapVO> sc = NetworkSearch.create();
        sc.setParameters(ApiConstants.NETWORK_ID, networkId);
        return listBy(sc);
    }
}
