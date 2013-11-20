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

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

public class XSerializerXmlAdapter implements XSerializerAdapter {

    private XSerializer serializer;

    public XSerializerXmlAdapter() {
    }

    @Override
    public void setSerializer(XSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void beginElement(String element, String namespace, int indentLevel, PrintWriter writer) {
        indent(true, indentLevel, writer);
        writer.print("<" + element);
        if (namespace != null && !namespace.isEmpty()) {
            writer.print(" xmlns=\"");
            writer.print(namespace);
            writer.print("\"");
        }
        writer.print(">");
    }

    @Override
    public void endElement(String element, int indentLevel, PrintWriter writer) {
        indent(true, indentLevel, writer);
        writer.print("</" + element + ">");
    }

    @Override
    public void writeElement(String elementName, String itemName, Object value, Field f, int indentLevel, PrintWriter writer) {
        Class<?> fieldType = f.getType();
        if (!serializer.isComposite(fieldType)) {
            if (fieldType == Date.class) {
                if (value != null) {
                    indent(true, indentLevel, writer);
                    writer.print("<" + elementName + ">");
                    writer.print(DateHelper.getGMTDateFormat("yyyy-MM-dd").format((Date)value) + "T" + DateHelper.getGMTDateFormat("HH:mm:ss").format((Date)value) + ".000Z");
                    writer.print("</" + elementName + ">");
                } else {
                    if (!serializer.omitNullField(f)) {
                        indent(true, indentLevel, writer);
                        writer.print("<" + elementName + "/>");
                    }
                }
            } else if (fieldType == Calendar.class) {
                if (value != null) {
                    indent(true, indentLevel, writer);

                    Date dt = ((Calendar)value).getTime();
                    writer.print("<" + elementName + ">");
                    writer.print(DateHelper.getGMTDateFormat("yyyy-MM-dd").format(dt) + "T" + DateHelper.getGMTDateFormat("HH:mm:ss").format(dt) + ".000Z");
                    writer.print("</" + elementName + ">");
                } else {
                    if (!serializer.omitNullField(f)) {
                        indent(true, indentLevel, writer);
                        writer.print("<" + elementName + "/>");
                    }
                }
            } else {
                if (value != null) {
                    indent(true, indentLevel, writer);
                    if (!value.toString().isEmpty()) {
                        writer.print("<" + elementName + ">");
                        writer.print(value);
                        writer.print("</" + elementName + ">");
                    } else {
                        writer.print("<" + elementName + "/>");
                    }
                } else {
                    if (!serializer.omitNullField(f)) {
                        indent(true, indentLevel, writer);
                        writer.print("<" + elementName + "/>");
                    }
                }
            }
        } else if (fieldType.isArray()) {
            if (value != null) {
                if (!serializer.flattenField(f)) {
                    indent(true, indentLevel, writer);
                    writer.print("<" + elementName + ">");
                    indentLevel++;
                }

                for (Object sub : (Object[])value) {
                    serializer.serializeTo(sub, itemName, null, indentLevel, writer);
                }

                if (!serializer.flattenField(f)) {
                    indentLevel--;
                    indent(true, indentLevel, writer);
                    writer.print("</" + elementName + ">");
                }
            } else {
                if (!serializer.omitNullField(f)) {
                    indent(true, indentLevel, writer);
                    writer.print("<" + elementName + "/>");
                }
            }
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            if (value != null) {
                if (!serializer.flattenField(f)) {
                    indent(true, indentLevel, writer);
                    writer.print("<" + elementName + ">");
                    indentLevel++;
                }

                Iterator it = ((Collection)value).iterator();
                if (it != null) {
                    while (it.hasNext()) {
                        Object sub = it.next();
                        serializer.serializeTo(sub, itemName, null, indentLevel, writer);
                    }
                }

                if (!serializer.flattenField(f)) {
                    indentLevel--;
                    indent(true, indentLevel, writer);
                    writer.print("</" + elementName + ">");
                }
            } else {
                if (!serializer.omitNullField(f)) {
                    indent(true, indentLevel, writer);
                    writer.print("<" + elementName + "/>");
                }
            }
        } else {
            serializer.serializeTo(value, elementName, null, indentLevel, writer);
        }
    }

    @Override
    public void writeSeparator(int indentLevel, PrintWriter writer) {
        // do nothing
    }

    private void indent(boolean newLine, int indentLevel, PrintWriter writer) {
        if (newLine)
            writer.println("");
        for (int i = 0; i < indentLevel; i++)
            writer.append("    ");
    }
}
