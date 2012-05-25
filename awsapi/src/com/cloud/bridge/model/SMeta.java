/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.model;

import java.io.Serializable;

/**
 * @author Kelven Yang
 */
public class SMeta implements Serializable {
	private static final long serialVersionUID = 7459503272337054283L;
	
	private Long id;
	private String target;
	private long targetId;
	
	private String name;
	private String value;

	public SMeta() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public String getTarget() {
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public long getTargetId() {
		return targetId;
	}
	
	public void setTargetId(long targetId) {
		this.targetId = targetId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		
		if(!(other instanceof SMeta))
			return false;
		
		return getTarget().equals(((SMeta)other).getTarget()) && getTargetId() == ((SMeta)other).getTargetId() 
			&& getName().equals(((SMeta)other).getName());
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		hashCode = hashCode*17 + getTarget().hashCode();
		hashCode = hashCode*17 + (int)getTargetId();
		hashCode = hashCode*17 + getName().hashCode();
		return hashCode;
	}
}
