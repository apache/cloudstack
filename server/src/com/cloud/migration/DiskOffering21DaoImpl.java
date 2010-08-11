package com.cloud.migration;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;

@Local(value={DiskOffering21Dao.class})
public class DiskOffering21DaoImpl extends GenericDaoBase<DiskOffering21VO, Long> implements DiskOffering21Dao {
}
