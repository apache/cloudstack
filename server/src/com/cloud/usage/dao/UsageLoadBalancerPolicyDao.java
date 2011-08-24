/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsageLoadBalancerPolicyVO;
import com.cloud.utils.db.GenericDao;

public interface UsageLoadBalancerPolicyDao extends GenericDao<UsageLoadBalancerPolicyVO, Long> {
	public void removeBy(long userId, long id);
	public void update(UsageLoadBalancerPolicyVO usage);
	public List<UsageLoadBalancerPolicyVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page);
}
