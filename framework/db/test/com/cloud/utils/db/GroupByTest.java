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

package com.cloud.utils.db;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;


public class GroupByTest {

    protected static final String EXPECTED_QUERY = "BASE GROUP BY FIRST(TEST_TABLE.TEST_COLUMN), MAX(TEST_TABLE.TEST_COLUMN) HAVING COUNT(TEST_TABLE2.TEST_COLUMN2) > ? ";
    @Test
    public void testToSql() {
        // Prepare
        final StringBuilder sb = new StringBuilder("BASE");
        final GroupByExtension groupBy = new GroupByExtension(new SearchBaseExtension(String.class, String.class));

        final Attribute att = new Attribute("TEST_TABLE", "TEST_COLUMN");
        final Pair<Func, Attribute> pair1 = new Pair<SearchCriteria.Func, Attribute>(SearchCriteria.Func.FIRST, att);
        final Pair<Func, Attribute> pair2 = new Pair<SearchCriteria.Func, Attribute>(SearchCriteria.Func.MAX, att);
        groupBy._groupBys = new ArrayList<Pair<Func, Attribute>>();
        groupBy._groupBys.add(pair1);
        groupBy._groupBys.add(pair2);
        groupBy.having(SearchCriteria.Func.COUNT, att, Op.GT, "SOME_VALUE");

        // Execute
        groupBy.toSql(sb);

        // Assert
        assertTrue("It didn't create the expected SQL query.", sb.toString().equals(EXPECTED_QUERY));
    }
}

class GroupByExtension extends GroupBy<SearchBaseExtension, String, String> {

    public GroupByExtension(final SearchBaseExtension builder) {
        super(builder);
        _builder = builder;
    }

    @Override
    protected void init(final SearchBaseExtension builder) {
    }
}

@SuppressWarnings({"rawtypes", "unchecked"})
class SearchBaseExtension extends SearchBase<SearchBaseExtension, String, String>{

    SearchBaseExtension(final Class entityType, final Class resultType) {
        super(entityType, resultType);
        _specifiedAttrs = new ArrayList<Attribute>();
        _specifiedAttrs.add(new Attribute("TEST_TABLE2", "TEST_COLUMN2"));
    }

    @Override
    protected void init(final Class<String> entityType, final Class<String> resultType) {
    }
}
