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

/**
 * SimpleQueryBuilder builds queries against a single table.  The
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
