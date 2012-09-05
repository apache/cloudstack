package com.cloud.bridge.persist.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.bridge.model.CloudStackConfigurationVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;


@Local(value={CloudStackConfigurationDao.class})
public class CloudStackConfigurationDaoImpl extends GenericDaoBase<CloudStackConfigurationVO, String> implements CloudStackConfigurationDao {
	private static final Logger s_logger = Logger.getLogger(CloudStackConfigurationDaoImpl.class);
	
	final SearchBuilder<CloudStackConfigurationVO> NameSearch= createSearchBuilder();
	
	public CloudStackConfigurationDaoImpl() { }
	
	
	@Override
	@DB
	public String getConfigValue(String name) {
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			SearchCriteria<CloudStackConfigurationVO> sc = NameSearch.create();
			sc.setParameters("name", name);
			return findOneBy(sc).getValue();
        }finally {
		
		}
	}
	
}
