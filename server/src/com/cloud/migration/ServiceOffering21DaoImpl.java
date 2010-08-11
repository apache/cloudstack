package com.cloud.migration;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;

@Local(value={ServiceOffering21Dao.class})
public class ServiceOffering21DaoImpl extends GenericDaoBase<ServiceOffering21VO, Long> implements ServiceOffering21Dao {
}
