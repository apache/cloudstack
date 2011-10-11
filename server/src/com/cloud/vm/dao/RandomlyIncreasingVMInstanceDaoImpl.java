/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.vm.dao;


import javax.ejb.Local;

import org.apache.log4j.Logger;

@Local(value = { UserVmDao.class })
public class RandomlyIncreasingVMInstanceDaoImpl extends UserVmDaoImpl {

    public static final Logger s_logger = Logger.getLogger(RandomlyIncreasingVMInstanceDaoImpl.class);

    @Override
    public <K> K getNextInSequence(final Class<K> clazz, final String name) {
    	return getRandomlyIncreasingNextInSequence(clazz, name);
    }
    
}
