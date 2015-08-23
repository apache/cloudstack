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

package com.cloud.utils.xmlobject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class XmlObject {
    private final Logger logger = Logger.getLogger(XmlObject.class.getName());
    private final Map<String, Object> elements = new HashMap<String, Object>();
    private String text;
    private String tag;

    XmlObject() {
    }

    public void removeAllChildren() {
        elements.clear();
    }

    public XmlObject(String tag) {
        this.tag = tag;
    }

    public XmlObject putElement(String key, Object e) {
        if (e == null) {
            throw new IllegalArgumentException(String.format("element[%s] can not be null", key));
        }
        Object old = elements.get(key);
        if (old == null) {
            //System.out.println(String.format("no %s, add new", key));
            elements.put(key, e);
        } else {
            if (old instanceof List) {
                //System.out.println(String.format("already list %s, add", key));
                ((List)old).add(e);
            } else {
                //System.out.println(String.format("not list list %s, add list", key));
                List lst = new ArrayList();
                lst.add(old);
                lst.add(e);
                elements.put(key, lst);
            }
        }

        return this;
    }

    public void removeElement(String key) {
        elements.remove(key);
    }

    private Object recurGet(XmlObject obj, Iterator<String> it) {
        String key = it.next();
        Object e = obj.elements.get(key);
        if (e == null) {
            return null;
        }

        if (!it.hasNext()) {
            return e;
        } else {
            if (!(e instanceof XmlObject)) {
                throw new CloudRuntimeException(String.format("%s doesn't reference to a XmlObject", it.next()));
            }
            return recurGet((XmlObject)e, it);
        }
    }

    public <T> T get(String elementStr) {
        String[] strs = elementStr.split("\\.");
        List<String> lst = new ArrayList<String>(strs.length);
        Collections.addAll(lst, strs);
        return (T)recurGet(this, lst.iterator());
    }

    public <T> List<T> getAsList(String elementStr) {
        Object e = get(elementStr);
        if (e instanceof List) {
            return (List<T>)e;
        }

        List lst = new ArrayList(1);
        if (e != null) {
            lst.add(e);
        }

        return lst;
    }

    public String getText() {
        return text;
    }

    public XmlObject setText(String text) {
        this.text = text;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public XmlObject setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tag);
        List<XmlObject> children = new ArrayList<XmlObject>();
        for (Map.Entry<String, Object> e : elements.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val instanceof String) {
                sb.append(String.format(" %s=\"%s\"", key, val.toString()));
            } else if (val instanceof XmlObject) {
                children.add((XmlObject)val);
            } else if (val instanceof List) {
                children.addAll((Collection<? extends XmlObject>)val);
            } else {
                throw new CloudRuntimeException(String.format("unsupported element type[tag:%s, class: %s], only allowed type of [String, List<XmlObject>, Object]", key,
                    val.getClass().getName()));
            }
        }

        if (!children.isEmpty() && text != null) {
            logger.info(String.format("element %s cannot have both text[%s] and child elements, set text to null", tag, text));
            text = null;
        }

        if (!children.isEmpty()) {
            sb.append(">");
            for (XmlObject x : children) {
                sb.append(x.dump());
            }
            sb.append(String.format("</%s>", tag));
        } else {
            if (text != null) {
                sb.append(">");
                sb.append(text);
                sb.append(String.format("</%s>", tag));
            } else {
                sb.append(" />");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<" + tag);
        for (Map.Entry<String, Object> e : elements.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (!(value instanceof String)) {
                continue;
            }
            sb.append(String.format(" %s=\"%s\"", key, value.toString()));
        }

        if (text == null || "".equals(text.trim())) {
            sb.append(" />");
        } else {
            sb.append(">").append(text).append(String.format("</ %s>", tag));
        }
        return sb.toString();
    }

    public <T> T evaluateObject(T obj) {
        Class<?> clazz = obj.getClass();
        try {
            do {
                Field[] fs = clazz.getDeclaredFields();
                for (Field f : fs) {
                    f.setAccessible(true);
                    Object value = get(f.getName());
                    f.set(obj, value);
                }
                clazz = clazz.getSuperclass();
            } while (clazz != null && clazz != Object.class);
            return obj;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
