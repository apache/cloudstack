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

public class XSerializerJsonAdapter implements XSerializerAdapter {
    private XSerializer serializer;

    public XSerializerJsonAdapter() {
    }

    @Override
    public void setSerializer(XSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void beginElement(String element, String namespace, int indentLevel, PrintWriter writer) {
        indent(true, indentLevel, writer);
        if (element != null && !element.isEmpty())
            writer.print(element + ": {");
        else
            writer.print("{");
    }

    @Override
    public void endElement(String element, int indentLevel, PrintWriter writer) {
        indent(true, indentLevel, writer);
        writer.print("}");
    }

    @Override
    public void writeElement(String elementName, String itemName, Object value, Field f, int indentLevel, PrintWriter writer) {
        Class<?> fieldType = f.getType();
        if (!serializer.isComposite(fieldType)) {
            if (fieldType == Date.class) {
                if (value != null) {
                    indent(true, indentLevel, writer);
                    writer.print(elementName + ":\"");
                    writer.print(DateHelper.getGMTDateFormat("yyyy-MM-dd").format((Date)value) + "T" + DateHelper.getGMTDateFormat("HH:mm:ss").format((Date)value) + ".000Z");
                    writer.print("\"");
                } else {
                    if (!serializer.omitNullField(f)) {
                        indent(true, indentLevel, writer);
                        writer.print(elementName + ":null");
                    }
                }
            } else if (fieldType == Calendar.class) {
                if (value != null) {
                    indent(true, indentLevel, writer);

                    Date dt = ((Calendar)value).getTime();
                    writer.print(elementName + ":\"");
                    writer.print(DateHelper.getGMTDateFormat("yyyy-MM-dd").format(dt) + "T" + DateHelper.getGMTDateFormat("HH:mm:ss").format(dt) + ".000Z");
                    writer.print("\"");
                } else {
                    if (!serializer.omitNullField(f)) {
                        indent(true, indentLevel, writer);
                        writer.print(elementName + ":null");
                    }
                }
            } else {
                if (value != null) {
                    indent(true, indentLevel, writer);
                    writer.print(elementName + ":\"");
                    writer.print(value);
                    writer.print("\"");
                } else {
                    if (!serializer.omitNullField(f)) {
                        indent(true, indentLevel, writer);
                        writer.print(elementName + ":null");
                    }
                }
            }
        } else if (fieldType.isArray()) {
            if (value != null) {
                indent(true, indentLevel, writer);
                writer.print(elementName + ":[");
                indentLevel++;

                Object[] array = (Object[])value;

                for (int i = 0; i < array.length; i++) {
                    serializer.serializeTo(array[i], "", null, indentLevel, writer);

                    if (i < array.length - 1)
                        writeSeparator(indentLevel, writer);
                }

                indentLevel--;
                indent(true, indentLevel, writer);
                writer.print("]");
            } else {
                if (!serializer.omitNullField(f)) {
                    indent(true, indentLevel, writer);
                    writer.print(elementName + ":null");
                }
            }
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            if (value != null) {
                indent(true, indentLevel, writer);
                writer.print(elementName + ":[");
                indentLevel++;

                Iterator it = ((Collection)value).iterator();
                if (it != null) {
                    while (it.hasNext()) {
                        Object sub = it.next();
                        serializer.serializeTo(sub, "", null, indentLevel, writer);

                        if (it.hasNext())
                            writeSeparator(indentLevel, writer);
                    }
                }

                indentLevel--;
                indent(true, indentLevel, writer);
                writer.print("]");
            } else {
                if (!serializer.omitNullField(f)) {
                    indent(true, indentLevel, writer);
                    writer.print(elementName + ":null");
                }
            }
        } else {
            serializer.serializeTo(value, elementName, null, indentLevel, writer);
        }
    }

    @Override
    public void writeSeparator(int indentLevel, PrintWriter writer) {
        writer.print(",");
    }

    private void indent(boolean newLine, int indentLevel, PrintWriter writer) {
        if (newLine)
            writer.println("");
        for (int i = 0; i < indentLevel; i++)
            writer.append("    ");
    }
}
