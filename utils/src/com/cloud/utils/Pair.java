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
package com.cloud.utils;

public class Pair<T, U> {
    T t;
    U u;
    
    public Pair(T t, U u) {
        this.t = t;
        this.u = u;
    }
    
    public T first() {
        return t;
    }
    
    public U second() {
        return u;
    }
    
    public U second(U value) {
    	u = value;
    	return u;
    }
    
    public T first(T value) {
    	t = value;
    	return t;
    }
    
    @Override
    // Note: This means any two pairs with null for both values will match each
    // other but what can I do?  This is due to stupid type erasure.
    public int hashCode() {
        return (t != null ? t.hashCode() : 0) | (u != null ? u.hashCode() : 0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        }
        Pair<?, ?> that = (Pair<?, ?>)obj;
        return (t != null ? t.equals(that.t) : that.t == null) && (u != null ? u.equals(that.u) : that.u == null);
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("P[");
        b.append((t != null) ? t.toString() : "null");
        b.append(":");
        b.append((u != null) ? u.toString() : "null");
        b.append("]");
        return b.toString();
    }
}
