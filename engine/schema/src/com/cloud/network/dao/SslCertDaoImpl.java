package com.cloud.network.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import javax.ejb.Local;
import java.util.List;



@Local(value = {SslCertDao.class})
public class SslCertDaoImpl extends GenericDaoBase<SslCertVO, Long> implements SslCertDao {

    private final SearchBuilder<SslCertVO> listByAccountId;

    public SslCertDaoImpl() {
        listByAccountId = createSearchBuilder();
        listByAccountId.and("accountId", listByAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        listByAccountId.done();
    }

    @Override
    public List<SslCertVO> listByAccountId(Long accountId) {
        SearchCriteria<SslCertVO> sc = listByAccountId.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }


}
