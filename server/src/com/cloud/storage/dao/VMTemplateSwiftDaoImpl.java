/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

package com.cloud.storage.dao;

import java.util.Collections;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateSwiftVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * 
 * @author Anthony Xu
 * 
 */

@Local(value = { VMTemplateSwiftDao.class })
public class VMTemplateSwiftDaoImpl extends GenericDaoBase<VMTemplateSwiftVO, Long> implements VMTemplateSwiftDao {
    public static final Logger s_logger = Logger.getLogger(VMTemplateSwiftDaoImpl.class.getName());

    protected final SearchBuilder<VMTemplateSwiftVO> AllFieldSearch;

    public VMTemplateSwiftDaoImpl() {
        AllFieldSearch = createSearchBuilder();
        AllFieldSearch.and("swift_id", AllFieldSearch.entity().getSwiftId(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("template_id", AllFieldSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        AllFieldSearch.done();

    }

    @Override
    public List<VMTemplateSwiftVO> listBySwiftId(long id) {
        SearchCriteria<VMTemplateSwiftVO> sc = AllFieldSearch.create();
        sc.setParameters("swift_id", id);
        return listBy(sc);
    }

    @Override
    public List<VMTemplateSwiftVO> listByTemplateId(long templateId) {
        SearchCriteria<VMTemplateSwiftVO> sc = AllFieldSearch.create();
        sc.setParameters("template_id", templateId);
        return listBy(sc);
    }

    @Override
    public VMTemplateSwiftVO findOneByTemplateId(long templateId) {
        SearchCriteria<VMTemplateSwiftVO> sc = AllFieldSearch.create();
        sc.setParameters("template_id", templateId);
        List<VMTemplateSwiftVO> list = listBy(sc);
        if (list == null || list.size() < 1) {
            return null;
        } else {
            Collections.shuffle(list);
            return list.get(0);
        }
    }

    @Override
    public VMTemplateSwiftVO findBySwiftTemplate(long swiftId, long templateId) {
        SearchCriteria<VMTemplateSwiftVO> sc = AllFieldSearch.create();
        sc.setParameters("swift_id", swiftId);
        sc.setParameters("template_id", templateId);
        return findOneBy(sc);
    }

}
