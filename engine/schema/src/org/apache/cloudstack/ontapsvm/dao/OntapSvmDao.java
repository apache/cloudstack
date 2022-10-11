package org.apache.cloudstack.ontapsvm.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.ontapsvm.OntapSvmVO;

import java.util.List;

public interface OntapSvmDao extends GenericDao<OntapSvmVO, Long> {
    List<OntapSvmVO> getSvmsForNetwork(long networkId);

    List<String> getIpsForNetwork(long networkId);
}
