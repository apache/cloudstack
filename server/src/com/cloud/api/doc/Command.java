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

import java.util.ArrayList;

public class Command {
	
	private String name;
	private String description;
	private boolean isAsync;
	private String sinceVersion = null;
	private ArrayList<Argument> request;
	private ArrayList<Argument> response;
	
	public Command(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	public Command() {}

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

	public ArrayList<Argument> getRequest() {
		return request;
	}

	public void setRequest(ArrayList<Argument> request) {
		this.request = request;
	}

	public ArrayList<Argument> getResponse() {
		return response;
	}

	public void setResponse(ArrayList<Argument> response) {
		this.response = response;
	}
	
	public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

	public String getSinceVersion() {
		return sinceVersion;
	}

	public void setSinceVersion(String sinceVersion) {
		this.sinceVersion = sinceVersion;
	}
	
    public Argument getReqArgByName(String name){
		for (Argument a : this.getRequest()) {
			if (a.getName().equals(name)) {
                return a;
            }
		}
		return null;
	}
	
	public Argument getResArgByName(String name){
		for (Argument a : this.getResponse()) {
			if (a.getName().equals(name)) {
                return a;
            }
		}
		return null;
	}
	
}
