package com.cloud.ucs.database;

import javax.ejb.Local;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
@Local(value = { UcsBladeDao.class })
@DB(txn = false)
public class UcsBladeDaoImpl extends GenericDaoBase<UcsBladeVO, Long> implements UcsBladeDao {

}
