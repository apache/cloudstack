package com.cloud.utils.db;

import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

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
