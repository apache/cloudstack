/**
 * 
 */
package com.cloud.vm.dao;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.vm.NicVO;

@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    protected NicDaoImpl() {
        super();
    }
    
}
