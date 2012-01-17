/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.alert.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.alert.AlertVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Local(value = { AlertDao.class })
public class AlertDaoImpl extends GenericDaoBase<AlertVO, Long> implements AlertDao {
    @Override
    public AlertVO getLastAlert(short type, long dataCenterId, Long podId) {
        Filter searchFilter = new Filter(AlertVO.class, "createdDate", Boolean.FALSE, Long.valueOf(0), Long.valueOf(1));
        SearchCriteria<AlertVO> sc = createSearchCriteria();

        sc.addAnd("type", SearchCriteria.Op.EQ, Short.valueOf(type));
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, Long.valueOf(dataCenterId));
        if (podId != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        }

        List<AlertVO> alerts = listBy(sc, searchFilter);
        if ((alerts != null) && !alerts.isEmpty()) {
            return alerts.get(0);
        }
        return null;
    }
}
