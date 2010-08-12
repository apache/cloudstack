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

package com.cloud.pricing;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name=("pricing"))
public class PricingVO {
    public static final String PRICE_UNIT_HOURLY = "per hour";
    public static final String PRICE_UNIT_DAILY = "per day";
    public static final String PRICE_UNIT_WEEKLY = "per week";
    public static final String PRICE_UNIT_MONTHLY = "per month";
    public static final String PRICE_UNIT_MB = "per MB";
    public static final String PRICE_UNIT_GB = "per GB";

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
	private Long id = (long)-1;
	
	@Column(name="price")
	private float price = 0f;
	
	@Column(name="price_unit")
	private String priceUnit = PRICE_UNIT_HOURLY;

	@Column(name="type")
	private String type;
	
	@Column(name="type_id")
	private Long typeId;
	
	@Column(name=GenericDao.CREATED_COLUMN)
    Date created;
	
	public PricingVO() {
	}
	
    public PricingVO(Long id, float price, String priceUnit, String type, Long typeId, Date created) {
        this.id = id;
        this.price = price;
        this.priceUnit = priceUnit;
        this.type = type;
        this.typeId = typeId;
        this.created = created;
    }
	public PricingVO(float price, String priceUnit, String type, Long typeId, Date created) {
		this(null, price, priceUnit, type, typeId, created);
	}

	public Long getId() {
		return id;
	}

	public float getPrice() {
		return price;
	}

	public String getPriceUnit() {
		return priceUnit;
	}

	public String getType() {
		return type;
	}

	public Long getTypeId() {
		return typeId;
	}

	public Date getCreated() {
		return created;
	}
}
