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
package com.cloud.utils.net;

import com.cloud.utils.NumbersUtil;

/**
 * Simple Ip implementation class that works with both ip4 and ip6.
 *
 */
public class Ip {
    long ip;
    
    public Ip(long ip) {
        this.ip = ip;
    }
    
    public Ip(String ip) {
        this.ip = NetUtils.ip2Long(ip);
    }
    
    protected Ip() {
    }
    
    public String addr() {
        return toString();
    }
    
    public long longValue() {
        return ip;
    }
    
    @Override
    public String toString() {
        return NetUtils.long2Ip(ip);
    }
    
    public boolean isIp4() {
        return ip < Integer.MAX_VALUE;
    }
    
    public boolean isIp6() {
        return ip > Integer.MAX_VALUE;
    }
    
    @Override
    public int hashCode() {
        return NumbersUtil.hash(ip);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Ip) {
            return ip == ((Ip)obj).ip;
        } else if (obj instanceof String) {
            return ip == NetUtils.ip2Long((String)obj);
        } else if (obj instanceof Long) {
            return ip == (Long)obj;
        } else {
            return false;
        }
    }
}
