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

package com.cloud.configuration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.crypt.DBEncryptionUtil;

@Entity
@Table(name="configuration")
public class ConfigurationVO implements Configuration{
	@Column(name="instance")
    private String instance;

	@Column(name="component")
	private String component;
	
	@Id
	@Column(name="name")
    private String name;
	
	@Column(name="value", length=4095)
    private String value;
	
	@Column(name="description", length=1024)
    private String description;
	
	@Column(name="category")
	private String category;
	
	protected ConfigurationVO() {}
	
	public ConfigurationVO(String category, String instance, String component, String name, String value, String description) {
		this.category = category;
		this.instance = instance;
		this.component = component;
		this.name = name;
		this.value = value;
		this.description = description;
	}

	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public String getInstance() {
		return instance;
	}
	
	public void setInstance(String instance) {
		this.instance = instance;
	}

	public String getComponent() {
	    return component;
	}
	
	public void setComponent(String component) {
		this.component = component;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return ("Hidden".equals(getCategory()) ? DBEncryptionUtil.decrypt(value) : value);
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
}
