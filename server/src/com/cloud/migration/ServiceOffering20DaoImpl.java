package com.cloud.migration;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;

@Local(value={ServiceOffering20Dao.class})
public class ServiceOffering20DaoImpl extends GenericDaoBase<ServiceOffering20VO, Long> implements ServiceOffering20Dao  {
}
