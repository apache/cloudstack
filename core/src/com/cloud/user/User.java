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

package com.cloud.user;

import java.util.Date;

public interface User {
	public static final long UID_SYSTEM = 1; 
	
	public Long getId();
	
    public Date getCreated();
    
    public Date getRemoved();
    
	public String getUsername();
	
	public void setUsername(String username);
	
	public String getPassword();
	
	public void setPassword(String password);
	
	public String getFirstname();
	
	public void setFirstname(String firstname);
	
	public String getLastname();
	
	public void setLastname(String lastname);

	public long getAccountId();

	public void setAccountId(long accountId);

    public String getEmail();
    
    public void setEmail(String email);

    public String getState();

    public void setState(String state);

	public String getApiKey();
	
	public void setApiKey(String apiKey);
	
    public String getSecretKey();
    
    public void setSecretKey(String secretKey);
    
    public String getTimezone();
    
    public void setTimezone(String timezone);
    
}