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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

public class UpdateBuilder implements MethodInterceptor {
    protected final Map<String, Attribute> _attrs;
    protected Map<String, Ternary<Attribute, Boolean, Object>> _changes;
    
    protected UpdateBuilder(Map<String, Attribute> attrs) {
        _attrs = attrs;
        _changes = new HashMap<String, Ternary<Attribute, Boolean, Object>>();
    }
    
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        String name = method.getName();
        if (name.startsWith("set")) {
        	String field = methodToField(name, 3);
            makeChange(field, args[0]);
        } else if (name.startsWith("incr")) {
        	makeIncrChange(name, args);
        } else if (name.startsWith("decr")) {
        	makeDecrChange(name, args);
        }
        return methodProxy.invokeSuper(object, args);
    }
    
    private final String methodToField(String method, int start) {
    	char[] chs = method.toCharArray();
    	chs[start] = Character.toLowerCase(chs[start]);
    	return new String(chs, start, chs.length - start);
    }
    
    protected Attribute makeChange(String field, Object value) {
        Attribute attr = _attrs.get(field);
        
        assert (attr == null || attr.isUpdatable()) : "Updating an attribute that's not updatable: " + field;
        if (attr != null) {
            _changes.put(field, new Ternary<Attribute, Boolean, Object>(attr, null, value));
        }
        return attr;
    }
    
    protected void makeIncrChange(String method, Object[] args) {
    	String field = methodToField(method, 4);
    	Attribute attr = _attrs.get(field);
        assert (attr != null && attr.isUpdatable()) : "Updating an attribute that's not updatable: " + field;
    	incr(attr, args == null || args.length == 0 ? 1 : args[0]);
    }
    
    protected void makeDecrChange(String method, Object[] args) {
    	String field = methodToField(method, 4);
    	Attribute attr = _attrs.get(field);
        assert (attr != null && attr.isUpdatable()) : "Updating an attribute that's not updatable: " + field;
    	decr(attr, args == null || args.length == 0 ? 1 : args[0]);
    }
    
    public void set(Object entity, String name, Object value) {
    	Attribute attr = makeChange(name, value);

    	set(entity, attr, value);
    }
    
    public void set(Object entity, Attribute attr, Object value) {
        _changes.put(attr.field.getName(), new Ternary<Attribute, Boolean, Object>(attr, null, value));
        try {
			attr.field.set(entity, value);
		} catch (IllegalArgumentException e) {
			throw new CloudRuntimeException("Unable to update " + attr.field.getName() + " with " + value, e);
		} catch (IllegalAccessException e) {
			throw new CloudRuntimeException("Unable to update " + attr.field.getName() + " with " + value, e);
		}
    }
    
    public void incr(Attribute attr, Object value) {
        _changes.put(attr.field.getName(), new Ternary<Attribute, Boolean, Object>(attr, true, value));
    }
    
    public void decr(Attribute attr, Object value) {
        _changes.put(attr.field.getName(), new Ternary<Attribute, Boolean, Object>(attr, false, value));
    }
    
    public boolean hasChanges() {
        return _changes.size() != 0;
    }
    
    public boolean has(String name) {
        return _changes.containsKey(name);
    }
    
    public Object get(String name) {
    	return _changes.get(name).second();
    }
    
    protected void clear() {
        _changes.clear();
    }
    
    public StringBuilder toSql(String tables) {
    	List<Pair<Attribute, Object>> setters = new ArrayList<Pair<Attribute, Object>>();
    	if (_changes.isEmpty()) {
    		return null;
    	}
    	
    	return SqlGenerator.buildMysqlUpdateSql(tables, _changes.values());
    }
    
    public Collection<Ternary<Attribute, Boolean, Object>> getChanges() {
    	return _changes.values();
    }
}
