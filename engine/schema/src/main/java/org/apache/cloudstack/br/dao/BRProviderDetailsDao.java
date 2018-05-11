package org.apache.cloudstack.br.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.br.BRProviderDetailVO;

import java.util.List;

public interface BRProviderDetailsDao extends GenericDao<BRProviderDetailVO, Long> {

    void addDetails(List<BRProviderDetailVO> details);
    void removeDetails(long providerId);
}
