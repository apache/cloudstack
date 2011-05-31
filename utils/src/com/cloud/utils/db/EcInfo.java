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

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.JoinColumn;

import com.cloud.utils.exception.CloudRuntimeException;

public class EcInfo {
    protected String insertSql;
    protected String selectSql;
    protected String clearSql;
    protected Class<?> targetClass;
    protected Class<?> rawClass;

    public EcInfo(Attribute attr, Attribute idAttr) {
        attr.attache = this;
        ElementCollection ec = attr.field.getAnnotation(ElementCollection.class);
        targetClass = ec.targetClass();
        Class<?> type = attr.field.getType();
        if (type.isArray()) {
            rawClass = null;
        } else {
            ParameterizedType pType = (ParameterizedType)attr.field.getGenericType();
            Type rawType = pType.getRawType();
            Class<?> rawClazz = (Class<?>)rawType;
            try {
                if (!Modifier.isAbstract(rawClazz.getModifiers()) && !rawClazz.isInterface() && rawClazz.getConstructors().length != 0 && rawClazz.getConstructor() != null) {
                    rawClass = rawClazz;
                } else if (Set.class == rawClazz) {
                    rawClass = HashSet.class;
                } else if (List.class == rawClazz) {
                    rawClass = ArrayList.class;
                } else if (Collection.class == Collection.class) {
                    rawClass = ArrayList.class;
                } else {
                    assert (false) : " We don't know how to create this calss " + rawType.toString() + " for " + attr.field.getName();
                }
            } catch (NoSuchMethodException e) {
                throw new CloudRuntimeException("Write your own support for " + rawClazz + " defined by " + attr.field.getName());
            }
        }

        CollectionTable ct = attr.field.getAnnotation(CollectionTable.class);
        assert (ct.name().length() > 0) : "Please sepcify the table for " + attr.field.getName();
        StringBuilder selectBuf = new StringBuilder("SELECT ");
        StringBuilder insertBuf = new StringBuilder("INSERT INTO ");
        StringBuilder clearBuf = new StringBuilder("DELETE FROM ");

        clearBuf.append(ct.name()).append(" WHERE ");
        selectBuf.append(attr.columnName);
        selectBuf.append(" FROM ").append(ct.name()).append(", ").append(attr.table);
        selectBuf.append(" WHERE ");

        insertBuf.append(ct.name()).append("(");
        StringBuilder valuesBuf = new StringBuilder("SELECT ");

        for (JoinColumn jc : ct.joinColumns()) {
            selectBuf.append(ct.name()).append(".").append(jc.name()).append("=");
            if (jc.referencedColumnName().length() == 0) {
                selectBuf.append(idAttr.table).append(".").append(idAttr.columnName);
                valuesBuf.append(idAttr.table).append(".").append(idAttr.columnName);
                clearBuf.append(ct.name()).append(".").append(jc.name()).append("=?");
            } else {
                selectBuf.append(attr.table).append(".").append(jc.referencedColumnName());
                valuesBuf.append(attr.table).append(".").append(jc.referencedColumnName()).append(",");
            }
            selectBuf.append(" AND ");
            insertBuf.append(jc.name()).append(", ");
            valuesBuf.append(", ");
        }

        selectSql = selectBuf.append(idAttr.table).append(".").append(idAttr.columnName).append("=?").toString();
        insertBuf.append(attr.columnName).append(") ");
        valuesBuf.append("? FROM ").append(attr.table);
        valuesBuf.append(" WHERE ").append(idAttr.table).append(".").append(idAttr.columnName).append("=?");

        insertSql = insertBuf.append(valuesBuf).toString();
        clearSql = clearBuf.toString();
    }
}
