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

package com.cloud.utils.mgmt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

public class PropertyMapDynamicBean implements DynamicMBean {
	
	private Map<String, Object> _propMap = new HashMap<String, Object>();
	
	public PropertyMapDynamicBean() {
	}
	
	public PropertyMapDynamicBean(Map<String, Object> propMap) {
		_propMap = propMap;
	}

	@Override
	public synchronized Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
		if(_propMap != null) {
			return _propMap.get(name);
		}
		
		throw new AttributeNotFoundException("No such property " + name);	
	}

	@Override
	public synchronized AttributeList getAttributes(String[] names) {
		AttributeList list = new AttributeList();
        for (String name : names) {
            Object value = _propMap.get(name);
            if (value != null)
                list.add(new Attribute(name, value));
        }
        return list;		
	}

	@Override
	public synchronized MBeanInfo getMBeanInfo() {
		SortedSet<String> names = new TreeSet<String>();
		
        for (String name : _propMap.keySet())
            names.add(name);
        
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[names.size()];
        Iterator<String> it = names.iterator();
        for (int i = 0; i < attrs.length; i++) {
            String name = it.next();
            attrs[i] = new MBeanAttributeInfo(
                    name,
                    "java.lang.String",
                    name,
                    true,   // isReadable
                    true,   // isWritable
                    false); // isIs
        }
        
        return new MBeanInfo(
            this.getClass().getName(),
            "Dynamic MBean",
            attrs,
            null,
            null,
            null);	
    }

	@Override
	public synchronized Object invoke(String name, Object[] args, String[] sig) throws MBeanException, ReflectionException {
		throw new ReflectionException(new NoSuchMethodException(name));	
	}

	@Override
	public synchronized void setAttribute(Attribute attr) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		String name = attr.getName();
		if(name != null)
			_propMap.put(name, attr.getValue());
	}

	@Override
	public synchronized AttributeList setAttributes(AttributeList list) {
		Attribute[] attrs = (Attribute[]) list.toArray(new Attribute[0]);
		AttributeList retList = new AttributeList();
        for (Attribute attr : attrs) {
            String name = attr.getName();
            Object value = attr.getValue();
            _propMap.put(name, value);
            retList.add(new Attribute(name, value));
        } 
        return retList;
	}
	
	public synchronized void addProp(String name, Object value) {
		_propMap.put(name, value);
	}
	
	public synchronized Object getProp(String name) {
		return _propMap.get(name);
	}
}
