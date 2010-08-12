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

import javax.ejb.Local;

import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Local(value={UserAccountDao.class})
public class UserAccountDaoImpl extends GenericDaoBase<UserAccountVO, Long> implements UserAccountDao {
    @Override
    public UserAccount getUserAccount(String username, Long domainId) {
        if ((username == null) || (domainId == null)) {
            return null;
        }

        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("username", SearchCriteria.Op.EQ, username);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        return findOneActiveBy(sc);
    }

    @Override
    public boolean validateUsernameInDomain(String username, Long domainId) {
        UserAccount userAcct = getUserAccount(username, domainId);
        if (userAcct == null) {
            return true;
        }
        return false;
    }
}
