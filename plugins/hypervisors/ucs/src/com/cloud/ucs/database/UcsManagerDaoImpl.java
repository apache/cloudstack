package com.cloud.ucs.database;

import javax.ejb.Local;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;

@Local(value = { UcsManagerDao.class })
@DB(txn = false)
public class UcsManagerDaoImpl  extends GenericDaoBase<UcsManagerVO, Long> implements UcsManagerDao {
}

