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
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.any;


public class GroupByTest {

    public static GroupBy<SearchBaseExtension, String, String> mockGroupBy1(final SearchBaseExtension builder) {
        GroupBy<SearchBaseExtension, String, String> mockInstance = spy(new GroupBy(builder));
        mockInstance._builder = builder;
        doNothing().when(mockInstance).init(any(SearchBaseExtension.class));
        return mockInstance;
    }

    protected static final String EXPECTED_QUERY = "BASE GROUP BY FIRST(TEST_TABLE.TEST_COLUMN), MAX(TEST_TABLE.TEST_COLUMN) HAVING COUNT(TEST_TABLE2.TEST_COLUMN2) > ? ";
    protected static final DbTestDao dao = new DbTestDao();
    protected static final String EXPECTED_QUERY_2 = "TEST GROUP BY test.fld_int HAVING SUM(test.fld_long) > ? ";
    protected static final String FULL_EXPECTED_QUERY_2 = "SELECT test.fld_string FROM test WHERE test.fld_string = ?  GROUP BY test.fld_int HAVING SUM(test.fld_long) > ? ";

    @Test
    public void testToSql() {
        // Prepare
        final StringBuilder sb = new StringBuilder("BASE");
        // Construct mock object
        final GroupBy<SearchBaseExtension, String, String> groupBy = GroupByTest.mockGroupBy1(new SearchBaseExtension(String.class, String.class));

        final Attribute att = new Attribute("TEST_TABLE", "TEST_COLUMN");
        final Attribute att2 = new Attribute("TEST_TABLE2", "TEST_COLUMN2");
        final Pair<Func, Attribute> pair1 = new Pair<>(SearchCriteria.Func.FIRST, att);
        final Pair<Func, Attribute> pair2 = new Pair<>(SearchCriteria.Func.MAX, att);
        groupBy._groupBys = new ArrayList<>();
        groupBy._groupBys.add(pair1);
        groupBy._groupBys.add(pair2);
        groupBy.having(SearchCriteria.Func.COUNT, att2, Op.GT);

        // Execute
        groupBy.toSql(sb);

        // Assert
        assertTrue("It didn't create the expected SQL query.", sb.toString().equals(EXPECTED_QUERY));
    }

    @Test
    public void testToSqlWithDao() {
        StringBuilder sb = new StringBuilder("TEST");
        SearchBuilder<DbTestVO> searchBuilder = dao.createSearchBuilder();
        searchBuilder.selectFields(searchBuilder.entity().getFieldString());
        searchBuilder.and("st", searchBuilder.entity().getFieldString(), SearchCriteria.Op.EQ);
        GroupBy groupBy = searchBuilder.groupBy(searchBuilder.entity().getFieldInt());
        groupBy.having(SearchCriteria.Func.SUM, dao.getAllAttributes().get("fieldLong"), SearchCriteria.Op.GT);
        groupBy.toSql(sb);
        assertTrue("It didn't create the expected SQL query.", sb.toString().equals(EXPECTED_QUERY_2));

        searchBuilder.done();
        SearchCriteria<DbTestVO> sc = searchBuilder.create();
        sc.setGroupByValues(0);
        sc.setParameters("st", "SOMETHING");

        String clause = sc.getWhereClause();
        if (clause != null && clause.length() == 0) {
            clause = null;
        }

        final StringBuilder str = dao.createPartialSelectSql(sc, clause != null);
        if (clause != null) {
            str.append(clause);
        }

        Collection<JoinBuilder<SearchCriteria<?>>> joins;
        joins = sc.getJoins();
        if (joins != null) {
            dao.addJoins(str, joins);
        }

        List<Object> groupByValues = dao.addGroupBy(str, sc);

        assertTrue("It didn't create the expected SQL query.", str.toString().equals(FULL_EXPECTED_QUERY_2));
        assertTrue("Incorrect group by parameter list", groupByValues.size() == 1);
    }

}


class SearchBaseExtension extends SearchBase<SearchBaseExtension, String, String>{

    SearchBaseExtension(final Class entityType, final Class resultType) {
        super(entityType, resultType);
        _specifiedAttrs = new ArrayList<>();
    }

    @Override
    protected void init(final Class<String> entityType, final Class<String> resultType) {
    }
}
