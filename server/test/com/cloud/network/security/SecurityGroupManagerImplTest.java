/**
 * 
 */
package com.cloud.network.security;

import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.network.security.SecurityGroupManagerImpl.CidrComparator;

/**
 * @author daan
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/SecurityGroupManagerTestContext.xml")
public class SecurityGroupManagerImplTest extends TestCase {
    @Inject
    SecurityGroupManagerImpl2 _sgMgr = null;
    Set<String> cidrs;
    @Before
    public void setup() throws Exception {
        cidrs =  new TreeSet<String>(new CidrComparator());
    }
    @Test (expected=NumberFormatException.class)
    public void emptyCidrCompareTest() {
        cidrs.add("");
        cidrs.add("");
    }
    @Test (expected=NumberFormatException.class)
    public void faultyCidrCompareTest() {
        cidrs.add("111.222.333.444");
        cidrs.add("111.222.333.444");
    }
    @Test
    public void sameCidrCompareTest() {
        cidrs.add("1.2.3.4/5");
        cidrs.add("1.2.3.4/5");
        assertEquals("only one element expected",1,cidrs.size());
        CidrComparator cmp = new CidrComparator();
        assertEquals("should be 0",0,cmp.compare("1.2.3.4/5","1.2.3.4/5"));
    }
    @Test
    public void CidrCompareTest() {
        cidrs.add("1.2.3.4/5");
        cidrs.add("1.2.3.4/6");
        assertEquals("two element expected",2,cidrs.size());
        CidrComparator cmp = new CidrComparator();
        assertEquals("should be 1",1,cmp.compare("1.2.3.4/5","1.2.3.4/6"));
        assertEquals("should be -2",-2,cmp.compare("1.2.3.4/5","1.2.3.4/3"));
    }
}
