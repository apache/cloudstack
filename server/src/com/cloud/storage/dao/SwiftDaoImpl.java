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

import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.SwiftVO;
import com.cloud.utils.db.GenericDaoBase;

/**
 * 
 * @author Anthony Xu
 * 
 */

@Local (value={SwiftDao.class})
public class SwiftDaoImpl extends GenericDaoBase<SwiftVO, Long> implements SwiftDao {
    public static final Logger s_logger = Logger.getLogger(SwiftDaoImpl.class.getName());

    @Override
    public SwiftTO getSwiftTO(Long swiftId) {
        if (swiftId != null) {
            SwiftVO swift = findById(swiftId);
            if (swift != null) {
                return swift.toSwiftTO();
            }
            return null;
        }

        List<SwiftVO> swiftVOs = listAll();
        if (swiftVOs == null || swiftVOs.size() < 1) {
            return null;
        } else {
            Collections.shuffle(swiftVOs);
            return swiftVOs.get(0).toSwiftTO();
        }
    }
}