package com.cloud.ucs.database;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria2;

public interface UcsManagerDao extends GenericDao<UcsManagerVO, Long> {
}
