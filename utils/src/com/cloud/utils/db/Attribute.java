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

import java.lang.reflect.Field;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * The Java annotation are somewhat incomplete.  This gives better information
 * about exactly what each field has.
 *
 */
public class Attribute {
    public enum Flag {
        Insertable(0x01),
        Updatable(0x02),
        Nullable(0x04),
        DaoGenerated(0x08),
        DbGenerated(0x10),
        Embedded(0x20),
        Id(0x40),
        Selectable(0x80),
        Time(0x100),
        Date(0x200),
        TimeStamp(0x400),
        SequenceGV(0x1000),
        TableGV(0x2000),
        AutoGV(0x4000),
        Created(0x10000),
        Removed(0x20000),
        DC(0x40000),
        CharDT(0x100000),
        StringDT(0x200000),
        IntegerDT(0x400000),
        Encrypted(0x800000);

        int place;
        Flag(int place) {
            this.place = place;
        }

        public int place() {
            return place;
        }

        public boolean check(int value) {
            return (value & place) == place;
        }

        public int setTrue(int value) {
            return (value | place);
        }

        public int setFalse(int value) {
            return (value & ~place);
        }
    }

    protected String table;
    protected String columnName;
    protected Field field;
    protected int flags;
    protected Column column;
    protected Object attache;

    public Attribute(Class<?> clazz, AttributeOverride[] overrides, Field field, String tableName, boolean isEmbedded, boolean isId) {
        this.field = field;
        flags = 0;
        table = tableName;
        setupColumnInfo(clazz, overrides, tableName, isEmbedded, isId);
    }

    public Attribute(String table, String columnName) {
        this.table = table;
        this.columnName = columnName;
        this.field = null;
        this.column = null;
    }

    protected void setupColumnInfo(Class<?> clazz, AttributeOverride[] overrides, String tableName, boolean isEmbedded, boolean isId) {
        flags = Flag.Selectable.setTrue(flags);
        GeneratedValue gv = field.getAnnotation(GeneratedValue.class);
        if (gv != null) {
            if (gv.strategy() == GenerationType.IDENTITY) {
                flags = Flag.DbGenerated.setTrue(flags);
            } else if (gv.strategy() == GenerationType.SEQUENCE) {
                assert (false) : "Sequence generation not supported.";
                flags = Flag.DaoGenerated.setTrue(flags);
                flags = Flag.Insertable.setTrue(flags);
                flags = Flag.SequenceGV.setTrue(flags);
            } else if (gv.strategy() == GenerationType.TABLE) {
                flags = Flag.DaoGenerated.setTrue(flags);
                flags = Flag.Insertable.setTrue(flags);
                flags = Flag.TableGV.setTrue(flags);
            } else if (gv.strategy() == GenerationType.AUTO) {
                flags = Flag.DaoGenerated.setTrue(flags);
                flags = Flag.Insertable.setTrue(flags);
                flags = Flag.AutoGV.setTrue(flags);
            }
        }
        
        if (isEmbedded) {
            flags = Flag.Embedded.setTrue(flags);
        }

        if (isId) {
            flags = Flag.Id.setTrue(flags);
        } else {
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
                flags = Flag.Id.setTrue(flags);
            }
        }
        column = field.getAnnotation(Column.class);
        if (gv == null) {
            if (column == null || (column.insertable() && column.table().length() == 0)) {
                flags = Flag.Insertable.setTrue(flags);
            }
            if (column == null || (column.updatable() && column.table().length() == 0)) {
                flags = Flag.Updatable.setTrue(flags);
            }
            if (column == null || column.nullable()) {
                flags = Flag.Nullable.setTrue(flags);
            }
            if (column != null && column.encryptable()) {
                flags = Flag.Encrypted.setTrue(flags);
            }
        }
        ElementCollection ec = field.getAnnotation(ElementCollection.class);
        if (ec != null) {
            flags = Flag.Insertable.setFalse(flags);
            flags = Flag.Selectable.setFalse(flags);
        }

        Temporal temporal = field.getAnnotation(Temporal.class);
        if (temporal != null) {
            if (temporal.value() == TemporalType.DATE) {
                flags = Flag.Date.setTrue(flags);
            } else if (temporal.value() == TemporalType.TIME) {
                flags = Flag.Time.setTrue(flags);
            } else if (temporal.value() == TemporalType.TIMESTAMP) {
                flags = Flag.TimeStamp.setTrue(flags);
            }
        }

        if (column != null && column.table().length() > 0) {
            table = column.table();
        }

        columnName = DbUtil.getColumnName(field, overrides);
    }

    public final boolean isInsertable() {
        return Flag.Insertable.check(flags);
    }

    public final boolean isUpdatable() {
        return Flag.Updatable.check(flags);
    }

    public final boolean isNullable() {
        return Flag.Nullable.check(flags);
    }

    public final boolean isId() {
        return Flag.Id.check(flags);
    }

    public final boolean isSelectable() {
        return Flag.Selectable.check(flags);
    }

    public final boolean is(Flag flag) {
        return flag.check(flags);
    }

    public final void setTrue(Flag flag) {
        flags = flag.setTrue(flags);
    }

    public final void setFalse(Flag flag) {
        flags = flag.setFalse(flags);
    }

    public final boolean isEncrypted() {
        return Flag.Encrypted.check(flags);
    }
    
    public Field getField() {
        return field;
    }

    public Object get(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            assert (false) : "How did we get here?";
            return null;
        }
    }

    @Override
    public int hashCode() {
        return columnName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Attribute)) {
            return false;
        }

        Attribute that = (Attribute)obj;

        return columnName.equals(that.columnName) && table.equals(that.table);
    }

    @Override
    public String toString() {
        return table + "." + columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
