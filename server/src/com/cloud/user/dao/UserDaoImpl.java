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

import javax.ejb.Local;

import com.cloud.user.UserVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * Implementation of the UserDao
 * 
 * @author Will Chan
 *
 */
@Local(value={UserDao.class})
public class UserDaoImpl extends GenericDaoBase<UserVO, Long> implements UserDao {
    protected SearchBuilder<UserVO> UsernamePasswordSearch;
    protected SearchBuilder<UserVO> UsernameSearch;
    protected SearchBuilder<UserVO> UsernameLikeSearch;
    protected SearchBuilder<UserVO> UserIdSearch;
    protected SearchBuilder<UserVO> AccountIdSearch;
    protected SearchBuilder<UserVO> SecretKeySearch;
    protected SearchBuilder<UserVO> RegistrationTokenSearch;
    
    protected UserDaoImpl () {
    	UsernameSearch = createSearchBuilder();
    	UsernameSearch.and("username", UsernameSearch.entity().getUsername(), SearchCriteria.Op.EQ);
    	UsernameSearch.done();
    	
        UsernameLikeSearch = createSearchBuilder();
        UsernameLikeSearch.and("username", UsernameLikeSearch.entity().getUsername(), SearchCriteria.Op.LIKE);
        UsernameLikeSearch.done();
        
        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("account", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();
        
        UsernamePasswordSearch = createSearchBuilder();
        UsernamePasswordSearch.and("username", UsernamePasswordSearch.entity().getUsername(), SearchCriteria.Op.EQ);
        UsernamePasswordSearch.and("password", UsernamePasswordSearch.entity().getPassword(), SearchCriteria.Op.EQ);
        UsernamePasswordSearch.done();

        UserIdSearch = createSearchBuilder();
        UserIdSearch.and("id", UserIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        UserIdSearch.done();

        SecretKeySearch = createSearchBuilder();
        SecretKeySearch.and("secretKey", SecretKeySearch.entity().getSecretKey(), SearchCriteria.Op.EQ);
        SecretKeySearch.done();
        
        RegistrationTokenSearch = createSearchBuilder();
        RegistrationTokenSearch.and("registrationToken", RegistrationTokenSearch.entity().getRegistrationToken(), SearchCriteria.Op.EQ);
        RegistrationTokenSearch.done();
    }

	@Override
	public UserVO getUser(String username, String password) {
	    SearchCriteria<UserVO> sc = UsernamePasswordSearch.create();
	    sc.setParameters("username", username);
	    sc.setParameters("password", password);
	    return findOneBy(sc);
	}
	
	@Override
    public List<UserVO> listByAccount(long accountId) {
	    SearchCriteria<UserVO> sc = AccountIdSearch.create();
	    sc.setParameters("account", accountId);
	    return listBy(sc, null);
	}

	@Override
	public UserVO getUser(String username) {
	    SearchCriteria<UserVO> sc = UsernameSearch.create();
	    sc.setParameters("username", username);
	    return findOneBy(sc);
	}

    @Override
    public UserVO getUser(long userId) {
        SearchCriteria<UserVO> sc = UserIdSearch.create();
        sc.setParameters("id", userId);
        return findOneBy(sc);
    }

	@Override
	public List<UserVO> findUsersLike(String username) {
        SearchCriteria<UserVO> sc = UsernameLikeSearch.create();
        sc.setParameters("username", "%" + username + "%");
        return listBy(sc);
    }
	
    @Override
    public UserVO findUserBySecretKey(String secretKey) {
        SearchCriteria<UserVO> sc = SecretKeySearch.create();
        sc.setParameters("secretKey", secretKey);
        return findOneBy(sc);
    }

	@Override
	public UserVO findUserByRegistrationToken(String registrationToken) {
        SearchCriteria<UserVO> sc = RegistrationTokenSearch.create();
        sc.setParameters("registrationToken", registrationToken);
        return findOneBy(sc);
	}
	
	@Override
	public List<UserVO> findUsersByName(String username) {
	    SearchCriteria<UserVO> sc = UsernameSearch.create();
        sc.setParameters("username", username);
        return listBy(sc);
	}
}
