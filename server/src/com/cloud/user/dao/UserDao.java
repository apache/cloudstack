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

package com.cloud.user.dao;

import java.util.List;

import com.cloud.user.UserVO;
import com.cloud.utils.db.GenericDao;


/*
 * Data Access Object for user table
 */
public interface UserDao extends GenericDao<UserVO, Long>{
	UserVO getUser(String username, String password);
	UserVO getUser(String username);
	UserVO getUser(long userId);
	List<UserVO> findUsersLike(String username);
	
	List<UserVO> listByAccount(long accountId);

	/**
	 * Finds a user based on the secret key provided.
	 * @param secretKey
	 * @return
	 */
	UserVO findUserBySecretKey(String secretKey);
	
	/**
	 * Finds a user based on the registration token provided.
	 * @param registrationToken
	 * @return
	 */
	UserVO findUserByRegistrationToken(String registrationToken);
	
	List<UserVO> findUsersByName(String username);
	
}
