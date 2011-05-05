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
package com.cloud.agent.api.to;
  
public class SwiftTO {
   
    String hostName;
    String account;
        
    String userName;
    String token;
            
    public SwiftTO() { }

    public SwiftTO(String hostName, String account, String userName, String token) {
        this.hostName = hostName;
        this.account = account;
        this.userName = userName;
        this.token = token;
    }
    
    public String getHostName() {
        return hostName;
    }

    public String getAccount() {
        return account;
    }

    public String getUserName() {
        return userName;
    }

    public String getToken() {
        return token;
    }
    

}
