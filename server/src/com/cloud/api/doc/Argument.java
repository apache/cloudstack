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

package com.cloud.api.doc;

import java.util.List;

public class Argument implements Comparable{
	private String name;
	private String description;
	private Boolean required;
	private String type;
	private String sinceVersion = null;
	private List<Argument> arguments;
	
	public Argument(String name) {
		this.name = name;
	}
	
	public Argument(String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
    }
	
	public Argument(String name, String description) {
        this.name = name;
        this.description = description;
    }
	
	public String getType() {
	    return this.type;
	}
	
	public void setType(String type) {
	    this.type = type;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Boolean isRequired() {
		return required;
	}
	public void setRequired(Boolean required) {
		this.required = required;
	}

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }
    
	public String getSinceVersion() {
		return sinceVersion;
	}

	public void setSinceVersion(String sinceVersion) {
		this.sinceVersion = sinceVersion;
	}
    
    public int compareTo(Object anotherAgrument) throws ClassCastException {
        if (!(anotherAgrument instanceof Argument))
            throw new ClassCastException("An Argument object expected.");
        Argument argument = (Argument)anotherAgrument;
        return this.getName().compareToIgnoreCase(argument.getName());    
    }
    
    public boolean hasArguments() {
        return (arguments!= null && !arguments.isEmpty());
    }

}
