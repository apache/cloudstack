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

public class Ternary<T, U, V> {
	private T t;
	private U u;
	private V v;
	
	
	public Ternary(T t, U u, V v) {
		this.t = t;
		this.u = u;
		this.v = v;
	}
	
	public T first() {
		return t;
	}
	
	public void first(T t) {
		this.t = t;
	}
	
	public U second() {
		return u;
	}
	
	public void second(U u) {
		this.u = u;
	}
	
	public V third() {
		return v;
	}
	
	public void third(V v) {
		this.v = v;
	}
	
    @Override
    // Note: This means any two pairs with null for both values will match each
    // other but what can I do?  This is due to stupid type erasure.
    public int hashCode() {
        return (t != null ? t.hashCode() : 0) | (u != null ? u.hashCode() : 0) | (v != null ? v.hashCode() : 0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        }
        Ternary<?, ?, ?> that = (Ternary<?, ?, ?>)obj;
        return (t != null ? t.equals(that.t) : that.t == null) && (u != null ? u.equals(that.u) : that.u == null) && (v != null ? v.equals(that.v): that.v == null);
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("T[");
        b.append(t != null ? t.toString() : "null");
        b.append(":");
        b.append(u != null ? u.toString() : "null");
        b.append(":");
        b.append(v != null ? v.toString() : "null");
        b.append("]");
        return b.toString();
    }
}
