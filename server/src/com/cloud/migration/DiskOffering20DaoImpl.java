package com.cloud.migration;

import javax.ejb.Local;
import com.cloud.utils.db.GenericDaoBase;

@Local(value={DiskOffering20Dao.class})
public class DiskOffering20DaoImpl extends GenericDaoBase<DiskOffering20VO, Long> implements DiskOffering20Dao {
}
