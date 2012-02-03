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
package com.cloud.vm;

import com.cloud.domain.Domain;
import com.cloud.domain.PartOf;
import com.cloud.user.Account;
import com.cloud.user.OwnedBy;
import com.cloud.user.User;
import com.cloud.utils.Journal;

/**
 * Specifies the entity that is calling the api.
 */
public interface ReservationContext extends PartOf, OwnedBy {
    /**
     * @return the user making the call.
     */
    User getCaller();

    /**
     * @return the account
     */
    Account getAccount();

    /**
     * @return the domain.
     */
    Domain getDomain();

    /**
     * @return the journal
     */
    Journal getJournal();

    /**
     * @return the reservation id.
     */
    String getReservationId();
}
