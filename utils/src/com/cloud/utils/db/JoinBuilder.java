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

package com.cloud.utils.db;


public class JoinBuilder<T> {
	
	public enum JoinType {
	     INNER ("INNER JOIN"),
	     LEFT ("LEFT JOIN"),
	     RIGHT ("RIGHT JOIN"),
	     RIGHTOUTER ("RIGHT OUTER JOIN"),
	     LEFTOUTER ("LEFT OUTER JOIN");
	     
	     private final String _name;
	     
	     JoinType(String name) {
	            _name = name;
	     }
	     
	     public String getName() { return _name; }
	}

	
	private T t;
	private JoinType type;
	private Attribute firstAttribute;
	private Attribute secondAttribute;
	
	public JoinBuilder(T t, Attribute firstAttribute,
			Attribute secondAttribute, JoinType type) {
		this.t = t;
		this.firstAttribute = firstAttribute;
		this.secondAttribute = secondAttribute;
		this.type = type;
	}
	
	public T getT() {
		return t;
	}
	public void setT(T t) {
		this.t = t;
	}
	public JoinType getType() {
		return type;
	}
	public void setType(JoinType type) {
		this.type = type;
	}
	public Attribute getFirstAttribute() {
		return firstAttribute;
	}
	public void setFirstAttribute(Attribute firstAttribute) {
		this.firstAttribute = firstAttribute;
	}
	public Attribute getSecondAttribute() {
		return secondAttribute;
	}
	public void setSecondAttribute(Attribute secondAttribute) {
		this.secondAttribute = secondAttribute;
	}

}


