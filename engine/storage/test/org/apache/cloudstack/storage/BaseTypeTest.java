package org.apache.cloudstack.storage;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class BaseTypeTest {
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(new TestType("a"), new TestType("A"))
                .addEqualityGroup(new TestType("Bd"), new TestType("bD"))
                .testEquals();
    }

    @Test
    public void testIsSameTypeAs() {
        Assert.assertTrue("'a' and 'A' should be considdered the same type", new TestType("a").isSameTypeAs("A"));
        Assert.assertTrue("'B' and 'b' should be considdered the same address", new TestType("B").isSameTypeAs(new TestType("b")));
    }
    class TestType extends BaseType {
        String content;
        public TestType(String t) {
            content = t;
        }
        @Override
        public String toString() {
            return content;
        }
    }
}
