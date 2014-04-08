//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

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
        if (_propMap != null) {
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
            attrs[i] = new MBeanAttributeInfo(name, "java.lang.String", name, true,   // isReadable
                true,   // isWritable
                false); // isIs
        }

        return new MBeanInfo(this.getClass().getName(), "Dynamic MBean", attrs, null, null, null);
    }

    @Override
    public synchronized Object invoke(String name, Object[] args, String[] sig) throws MBeanException, ReflectionException {
        throw new ReflectionException(new NoSuchMethodException(name));
    }

    @Override
    public synchronized void setAttribute(Attribute attr) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        String name = attr.getName();
        if (name != null)
            _propMap.put(name, attr.getValue());
    }

    @Override
    public synchronized AttributeList setAttributes(AttributeList list) {
        Attribute[] attrs = list.toArray(new Attribute[0]);
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
