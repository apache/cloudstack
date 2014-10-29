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
package com.cloud.bridge.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XSerializer {
    protected final static Logger logger = Logger.getLogger(XSerializer.class);

    private static Map<String, Class<?>> rootTypes = new HashMap<String, Class<?>>();

    private XSerializerAdapter adapter;
    private boolean flattenCollection;
    private boolean omitNull;

    public XSerializer(XSerializerAdapter adapter) {
        this.adapter = adapter;
        adapter.setSerializer(this);

        // set default serialization options
        flattenCollection = false;
        omitNull = false;
    }

    public XSerializer(XSerializerAdapter adapter, boolean flattenCollection, boolean omitNull) {
        this.adapter = adapter;
        adapter.setSerializer(this);

        this.flattenCollection = flattenCollection;
        this.omitNull = omitNull;
    }

    public boolean getFlattenCollection() {
        return flattenCollection;
    }

    public void setFlattenCollection(boolean value) {
        flattenCollection = value;
    }

    public boolean flattenField(Field f) {
        XFlatten flatten = f.getAnnotation(XFlatten.class);
        if (flatten != null)
            return flatten.value();
        return flattenCollection;
    }

    public boolean omitNullField(Field f) {
        XOmitNull omit = f.getAnnotation(XOmitNull.class);
        if (omit != null)
            return omit.value();

        return omitNull;
    }

    public boolean getOmitNull() {
        return omitNull;
    }

    public void setOmitNull(boolean value) {
        omitNull = value;
    }

    public static void registerRootType(String elementName, Class<?> clz) {
        rootTypes.put(elementName, clz);
    }

    public XSerializerAdapter getAdapter() {
        return adapter;
    }

    public static Object mapElement(String elementName) {
        Class<?> clz = rootTypes.get(elementName);
        if (clz == null) {
            logger.error("Object class is not registered for root element " + elementName);
            throw new IllegalArgumentException("Object class is not registered for root element " + elementName);
        }

        try {
            return clz.newInstance();
        } catch (InstantiationException e) {
            logger.error("Unable to instantiate object for root element due to InstantiationException, XML element: " + elementName);
            throw new IllegalArgumentException("Unable to instantiate object for root element " + elementName);
        } catch (IllegalAccessException e) {
            logger.error("Unable to instantiate object for root element due to IllegalAccessException, XML element: " + elementName);
            throw new IllegalArgumentException("Unable to instantiate object for root element due to IllegalAccessException, XML element: " + elementName);
        }
    }

    public Object serializeFrom(String xmlString) {
        try {
            Document doc = XmlHelper.parse(xmlString);
            Node node = XmlHelper.getRootNode(doc);
            if (node == null) {
                logger.error("Invalid XML document, no root element");
                return null;
            }

            Object object = mapElement(node.getNodeName());
            if (object == null) {
                logger.error("Unable to map root element. Please remember to use XSerializer.registerRootType() to register the root object type");
                return null;
            }

            if (object instanceof XSerializable)
                ((XSerializable)object).serializeFrom(this, object, node);
            else
                serializeFrom(object, object.getClass(), node);

            return object;
        } catch (IOException e) {
            logger.error("Unable to parse XML input due to " + e.getMessage(), e);
        }
        return null;
    }

    private void serializeFrom(Object object, Class<?> clz, Node node) {
        if (clz.getSuperclass() != null)
            serializeFrom(object, clz.getSuperclass(), node);

        Field[] fields = clz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];

            if ((f.getModifiers() & Modifier.STATIC) == 0) {
                f.setAccessible(true);

                Class<?> fieldType = f.getType();
                XElement elem = f.getAnnotation(XElement.class);
                if (elem == null)
                    continue;

                try {
                    if (fieldType.isPrimitive()) {
                        setPrimitiveField(f, object, XmlHelper.getChildNodeTextContent(node, elem.name()));
                    } else if (fieldType.getSuperclass() == Number.class) {
                        setNumberField(f, object, XmlHelper.getChildNodeTextContent(node, elem.name()));
                    } else if (fieldType == String.class) {
                        f.set(object, XmlHelper.getChildNodeTextContent(node, elem.name()));
                    } else if (fieldType == Date.class) {
                        setDateField(f, object, XmlHelper.getChildNodeTextContent(node, elem.name()));
                    } else if (fieldType == Calendar.class) {
                        setCalendarField(f, object, XmlHelper.getChildNodeTextContent(node, elem.name()));
                    } else if (fieldType.isArray()) {
                        if (flattenField(f))
                            setArrayField(f, object, node, elem.item(), elem.itemClass());
                        else
                            setArrayField(f, object, XmlHelper.getChildNode(node, elem.name()), elem.item(), elem.itemClass());
                    } else if (Collection.class.isAssignableFrom(fieldType)) {
                        if (flattenField(f))
                            setCollectionField(f, object, node, elem.item(), elem.itemClass());
                        else
                            setCollectionField(f, object, XmlHelper.getChildNode(node, elem.name()), elem.item(), elem.itemClass());
                    } else {
                        Node childNode = XmlHelper.getChildNode(node, elem.name());
                        Object fieldObject = f.get(object);
                        if (fieldObject == null) {
                            try {
                                fieldObject = fieldType.newInstance();
                            } catch (InstantiationException e) {
                                logger.error("Unable to instantiate " + fieldType.getName() + " object, please make sure it has public constructor");
                                assert (false);
                            }
                            f.set(object, fieldObject);
                        }
                        serializeFrom(fieldObject, fieldType, childNode);
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Unexpected exception " + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    logger.error("Unexpected exception " + e.getMessage(), e);
                }
            }
        }
    }

    private void setPrimitiveField(Field f, Object object, String valueContent) throws IllegalArgumentException, IllegalAccessException {

        String clzName = f.getType().getName();
        if (clzName.equals("boolean")) {
            if (valueContent != null && valueContent.equalsIgnoreCase("true"))
                f.setBoolean(object, true);
            else
                f.setBoolean(object, false);
        } else if (clzName.equals("byte")) {
            byte value = 0;
            if (valueContent != null)
                value = Byte.parseByte(valueContent);
            f.setByte(object, value);
        } else if (clzName.equals("char")) {
            char value = '\0';
            if (valueContent != null) {
                if (valueContent.charAt(0) == '\'')
                    value = valueContent.charAt(1);
                else
                    value = valueContent.charAt(0);
            }
            f.setChar(object, value);
        } else if (clzName.equals("short")) {
            short value = 0;
            if (valueContent != null)
                value = Short.parseShort(valueContent);
            f.setShort(object, value);
        } else if (clzName.equals("int")) {
            int value = 0;
            if (valueContent != null)
                value = Integer.parseInt(valueContent);
            f.setInt(object, value);
        } else if (clzName.equals("long")) {
            long value = 0;
            if (valueContent != null)
                value = Long.parseLong(valueContent);
            f.setLong(object, value);
        } else if (clzName.equals("float")) {
            float value = 0;
            if (valueContent != null)
                value = Float.parseFloat(valueContent);
            f.setFloat(object, value);
        } else if (clzName.equals("double")) {
            double value = 0;
            if (valueContent != null)
                value = Double.parseDouble(valueContent);
            f.setDouble(object, value);
        } else {
            logger.error("Assertion failed at setPrimitiveFiled");
            assert (false);
        }
    }

    private void setNumberField(Field f, Object object, String valueContent) throws IllegalArgumentException, IllegalAccessException {

        String clzName = f.getType().getName();
        if (clzName.equals("Byte")) {
            byte value = 0;
            if (valueContent != null)
                value = Byte.parseByte(valueContent);
            f.set(object, new Byte(value));
        } else if (clzName.equals("Short")) {
            short value = 0;
            if (valueContent != null)
                value = Short.parseShort(valueContent);
            f.set(object, new Short(value));
        } else if (clzName.equals("Integer")) {
            int value = 0;
            if (valueContent != null)
                value = Integer.parseInt(valueContent);
            f.set(object, new Integer(value));
        } else if (clzName.equals("Long")) {
            long value = 0;
            if (valueContent != null)
                value = Long.parseLong(valueContent);
            f.set(object, new Long(value));
        } else if (clzName.equals("Float")) {
            float value = 0;
            if (valueContent != null)
                value = Float.parseFloat(valueContent);
            f.set(object, new Float(value));
        } else if (clzName.equals("Double")) {
            double value = 0;
            if (valueContent != null)
                value = Double.parseDouble(valueContent);
            f.setDouble(object, new Double(value));
        } else if (clzName.equals("AtomicInteger")) {
            int value = 0;
            if (valueContent != null)
                value = Integer.parseInt(valueContent);
            f.set(object, new AtomicInteger(value));
        } else if (clzName.equals("AtomicLong")) {
            long value = 0;
            if (valueContent != null)
                value = Long.parseLong(valueContent);
            f.set(object, new AtomicLong(value));
        } else if (clzName.equals("BigInteger")) {
            logger.error("we don't support BigInteger for now");
            assert (false);
        } else if (clzName.equals("BigDecimal")) {
            logger.error("we don't support BigInteger for now");
            assert (false);
        } else {
            logger.error("Assertion failed at setPrimitiveFiled");
            assert (false);
        }
    }

    private void setDateField(Field f, Object object, String valueContent) throws IllegalArgumentException, IllegalAccessException {

        if (valueContent != null) {
            valueContent = valueContent.replace('T', ' ');
            valueContent = valueContent.replace('.', '\0');

            SimpleDateFormat df = DateHelper.getGMTDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date value = df.parse(valueContent);
                f.set(object, value);
            } catch (ParseException e) {
                logger.error("Unrecognized date/time format " + valueContent);
            }
        }
    }

    private void setCalendarField(Field f, Object object, String valueContent) throws IllegalArgumentException, IllegalAccessException {

        if (valueContent != null) {
            valueContent = valueContent.replace('T', ' ');
            valueContent = valueContent.replace('.', '\0');

            SimpleDateFormat df = DateHelper.getGMTDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date value = df.parse(valueContent);
                f.set(object, DateHelper.toCalendar(value));
            } catch (ParseException e) {
                logger.error("Unrecognized date/time format " + valueContent);
            }
        }
    }

    private void setArrayField(Field f, Object object, Node node, String itemElementName, String itemClass) throws IllegalArgumentException, IllegalAccessException {

        List<Object> arrayList = new ArrayList<Object>();

        Class<?> itemClz = null;
        try {
            itemClz = this.getClass().forName(itemClass);
        } catch (ClassNotFoundException e) {
            logger.error("Unable to find class " + itemClass);
            return;
        }

        if (node != null) {
            NodeList l = node.getChildNodes();
            if (l != null && l.getLength() > 0) {
                for (int i = 0; i < l.getLength(); i++) {
                    try {
                        Node itemNode = l.item(i);
                        if (itemNode.getNodeName().equals(itemElementName)) {
                            Object item = itemClz.newInstance();
                            serializeFrom(item, itemClz, l.item(i));
                            arrayList.add(item);
                        }
                    } catch (InstantiationException e) {
                        logger.error("Unable to initiate object instance for class " + itemClass + ", make sure it has public constructor");
                        break;
                    }
                }
            }
        }

        Object arrary = Array.newInstance(f.getType().getComponentType(), arrayList.size());
        arrayList.toArray((Object[])arrary);
        f.set(object, arrary);
    }

    private void setCollectionField(Field f, Object object, Node node, String itemElementName, String itemClass) throws IllegalArgumentException, IllegalAccessException {
        Object fieldObject = f.get(object);

        if (fieldObject == null) {
            logger.error("Please initialize collection field " + f.getName() + " in class " + object.getClass().getName() + "'s constructor");
            return;
        }

        Class<?> itemClz = null;
        try {
            itemClz = this.getClass().forName(itemClass);
        } catch (ClassNotFoundException e) {
            logger.error("Unable to find class " + itemClass);
            return;
        }

        NodeList l = node.getChildNodes();
        if (l != null && l.getLength() > 0) {
            for (int i = 0; i < l.getLength(); i++) {
                try {
                    Node itemNode = l.item(i);
                    if (itemNode.getNodeName().equals(itemElementName)) {
                        Object item = itemClz.newInstance();
                        serializeFrom(item, itemClz, l.item(i));
                        ((Collection)fieldObject).add(item);
                    }
                } catch (InstantiationException e) {
                    logger.error("Unable to initiate object instance for class " + itemClass + ", make sure it has public constructor");
                    break;
                }
            }
        }
    }

    public void serializeTo(Object obj, String startElement, String namespace, int indentLevel, PrintWriter writer) {
        if (startElement != null) {
            adapter.beginElement(startElement, namespace, indentLevel, writer);
            indentLevel++;
        }

        if (obj instanceof XSerializable) {
            ((XSerializable)obj).serializeTo(this, indentLevel, writer);
        } else {
            Class<?> clz = obj.getClass();
            serializeTo(obj, clz, indentLevel, writer);
        }

        if (startElement != null) {
            indentLevel--;
            adapter.endElement(startElement, indentLevel, writer);
        }
    }

    public String serializeTo(Object obj, String startElement, String namespace, int indentLevel) {
        StringWriter writer = new StringWriter();
        serializeTo(obj, startElement, namespace, indentLevel, new PrintWriter(writer));
        return writer.toString();
    }

    private void serializeTo(Object obj, Class<?> clz, int indentLevel, PrintWriter writer) {
        if (clz.getSuperclass() != null)
            serializeTo(obj, clz.getSuperclass(), indentLevel, writer);

        Field[] fields = clz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];

            if ((f.getModifiers() & Modifier.STATIC) == 0) {
                f.setAccessible(true);

                Class<?> fieldType = f.getType();
                XElement elem = f.getAnnotation(XElement.class);
                if (elem == null)
                    continue;

                Object fieldValue = null;
                try {
                    fieldValue = f.get(obj);
                } catch (IllegalArgumentException e) {
                    logger.error("Unexpected exception " + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    logger.error("Unexpected exception " + e.getMessage(), e);
                }

                adapter.writeElement(elem.name(), elem.item(), fieldValue, f, indentLevel, writer);
                if (i < fields.length - 1) {
                    Field next = fields[i + 1];
                    if ((next.getModifiers() & Modifier.STATIC) == 0 && next.getAnnotation(XElement.class) != null) {
                        adapter.writeSeparator(indentLevel, writer);
                    }
                }
            }
        }
    }

    public boolean isComposite(Class<?> clz) {
        if (clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class || clz == Calendar.class) {
            return false;
        }
        return true;
    }
}
