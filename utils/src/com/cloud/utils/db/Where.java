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

import java.util.List;

/**
 * Where implements any list of search conditions.
 *
 */
public class Where<T, K> implements FirstWhere<T, K>, NextWhere<T, K> {
    GenericSearchBuilder<T, K> _builder;
    List<Object> _conditions;
    
    protected Where(GenericSearchBuilder<T, K> builder) {
        _builder = builder;
    }
    
    @Override
    public Condition<T, K> field(Object useless, String as) {
        Attribute attr = _builder.getSpecifiedAttribute();
        Condition<T, K> cond = new Condition<T, K>(this, attr, as);
        _conditions.add(cond);
        return cond;
    }
    
    @Override
    public Where<T, K> and() {
        _conditions.add(" (");
        return this;
    }
    
    @Override
    public Where<T, K> or() {
        _conditions.add(" OR ");
        return this;
    }

    @Override
    public NextWhere<T, K> not() {
        _conditions.add(" NOT ");
        return this;
    }

    @Override
    public NextWhere<T, K> text(String text, String... paramNames) {
        assert ((paramNames.length == 0 && !text.contains("?")) || (text.matches("\\?.*{" + paramNames.length + "}")));
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Condition<T, K> field(Object useless) {
        return field(useless, null);
    }

    @Override
    public FirstWhere<T, K> op() {
        _conditions.add("(");
        return this;
    }

    @Override
    public GenericSearchBuilder<T, K> done() {
        return _builder;
    }
}
