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

package com.cloud.pricing.dao;

import java.sql.PreparedStatement;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.pricing.PricingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={PricingDao.class})
public class PricingDaoImpl extends GenericDaoBase<PricingVO, Long> implements PricingDao {
    private static final Logger s_logger = Logger.getLogger(PricingDaoImpl.class);
	protected SearchBuilder<PricingVO> TypeAndIdSearch;
	protected SearchBuilder<PricingVO> TypeSearch;
	
	protected static final String UPDATE_PRICING_SQL =
		"UPDATE pricing SET price = ?, price_unit = ? "
	+   "WHERE type = ?";

	public PricingDaoImpl() {
		TypeAndIdSearch = createSearchBuilder();
		TypeAndIdSearch.and("type", TypeAndIdSearch.entity().getType(), SearchCriteria.Op.EQ);
		TypeAndIdSearch.and("typeId", TypeAndIdSearch.entity().getTypeId(), SearchCriteria.Op.EQ);
		
		TypeSearch = createSearchBuilder();
		TypeSearch.and("type", TypeSearch.entity().getType(), SearchCriteria.Op.EQ);
	}
	
	public PricingVO findByTypeAndId(String type, Long typeId) {
		if (typeId == null) {
			SearchCriteria sc = TypeSearch.create();
			sc.setParameters("type", type);
			return findOneBy(sc);
		} else {
			SearchCriteria sc = TypeAndIdSearch.create();
			sc.setParameters("type", type);
			sc.setParameters("typeId", typeId);
			return findOneBy(sc);
		}
	}
	
	public void update(PricingVO pricing) {
        Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		try {
			String sql = UPDATE_PRICING_SQL;
			boolean useTypeId = false;
			if (pricing.getTypeId() != null) {
				useTypeId = true;
				sql = sql + " AND type_id = ?";
			}
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setFloat(1, pricing.getPrice());
			pstmt.setString(2, pricing.getPriceUnit());
			pstmt.setString(3, pricing.getType());
			
			if (useTypeId) {
				pstmt.setLong(4, pricing.getTypeId());
			}
			pstmt.executeUpdate();
		} catch (Exception e) {
			s_logger.warn(e);
		}
	}
}
