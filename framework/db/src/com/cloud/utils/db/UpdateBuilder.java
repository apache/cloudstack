// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

public class UpdateBuilder implements MethodInterceptor {
    protected Map<String, Ternary<Attribute, Boolean, Object>> _changes;
    protected HashMap<Attribute, Object> _collectionChanges;
    protected GenericDaoBase<?, ?> _dao;

    protected UpdateBuilder(GenericDaoBase<?, ?> dao) {
        _dao = dao;
        _changes = new HashMap<String, Ternary<Attribute, Boolean, Object>>();
    }

    @Override
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
        Attribute attr = _dao._allAttributes.get(field);

        assert (attr == null || attr.isUpdatable()) : "Updating an attribute that's not updatable: " + field;
        if (attr != null) {
            if (attr.attache == null) {
                _changes.put(field, new Ternary<Attribute, Boolean, Object>(attr, null, value));
            } else {
                if (_collectionChanges == null) {
                    _collectionChanges = new HashMap<Attribute, Object>();
                }
                _collectionChanges.put(attr, value);
            }
        }
        return attr;
    }

    protected void makeIncrChange(String method, Object[] args) {
        String field = methodToField(method, 4);
        Attribute attr = _dao._allAttributes.get(field);
        assert (attr != null && attr.isUpdatable()) : "Updating an attribute that's not updatable: " + field;
        incr(attr, args == null || args.length == 0 ? 1 : args[0]);
    }

    protected void makeDecrChange(String method, Object[] args) {
        String field = methodToField(method, 4);
        Attribute attr = _dao._allAttributes.get(field);
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
        return (_changes.size() + (_collectionChanges != null ? _collectionChanges.size() : 0)) != 0;
    }

    public boolean has(String name) {
        return _changes.containsKey(name);
    }

    public Map<Attribute, Object> getCollectionChanges() {
        return _collectionChanges;
    }

    public void clear() {
        _changes.clear();
        if (_collectionChanges != null) {
            _collectionChanges.clear();
            _collectionChanges = null;
        }
    }

    public StringBuilder toSql(String tables) {
        if (_changes.isEmpty()) {
            return null;
        }

        return SqlGenerator.buildMysqlUpdateSql(tables, _changes.values());
    }

    public Collection<Ternary<Attribute, Boolean, Object>> getChanges() {
        return _changes.values();
    }
}
