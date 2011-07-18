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

/**
 * SimpleQueryBuilder builds queries against a single table.  The
 * result is stored into the entity that represents the table.
 *
 */
public interface SimpleQueryBuilder<S> {
    /**
     * Select all of the columns in the entity object.  This is default so
     * it's not necessary to make this method call at all.
     */
    SimpleQueryBuilder<S> selectAll();
    
    /**
     * Select the following columns
     * @param columns array of columsn to select.
     */
    SimpleQueryBuilder<S> selectFields(Object... columns);
    
    /**
     * @return the entity object we're building this query for.  By using this
     * entity object, you can specify which column to select or form
     */
    S entity();
    
    /**
     * Starts the query conditionals.
     * @return
     */
    FirstWhere<S, ?> where();
}
