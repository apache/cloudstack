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

package com.cloud.network.security;

import com.cloud.network.security.SecurityGroupManagerImpl.CidrComparator;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;

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
        cidrs = new TreeSet<String>(new CidrComparator());
    }

    @Test(expected = NumberFormatException.class)
    public void emptyCidrCompareTest() {
        cidrs.add("");
        cidrs.add("");
    }

    @Test(expected = NumberFormatException.class)
    public void faultyCidrCompareTest() {
        cidrs.add("111.222.333.444");
        cidrs.add("111.222.333.444");
    }

    @Test
    public void sameCidrCompareTest() {
        cidrs.add("1.2.3.4/5");
        cidrs.add("1.2.3.4/5");
        assertEquals("only one element expected", 1, cidrs.size());
        CidrComparator cmp = new CidrComparator();
        assertEquals("should be 0", 0, cmp.compare("1.2.3.4/5", "1.2.3.4/5"));
    }

    @Test
    public void CidrCompareTest() {
        cidrs.add("1.2.3.4/5");
        cidrs.add("1.2.3.4/6");
        assertEquals("two element expected", 2, cidrs.size());
        CidrComparator cmp = new CidrComparator();
        assertEquals("should be 1", 1, cmp.compare("1.2.3.4/5", "1.2.3.4/6"));
        assertEquals("should be -2", -2, cmp.compare("1.2.3.4/5", "1.2.3.4/3"));
    }
}
