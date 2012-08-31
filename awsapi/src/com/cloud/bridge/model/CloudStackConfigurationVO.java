package com.cloud.bridge.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.DB;

@Entity
@Table(name="configuration")
public class CloudStackConfigurationVO {
	@Id
	@Column(name="name")
    private String name;
	
	@Column(name="value", length=4095)
    private String value;
	
	@DB
	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}


}
