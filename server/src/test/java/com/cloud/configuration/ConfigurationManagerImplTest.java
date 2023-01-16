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
package com.cloud.configuration;

import com.cloud.utils.net.NetUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;


@RunWith(PowerMockRunner.class)
@PrepareForTest(NetUtils.class)
public class ConfigurationManagerImplTest {
    ConfigurationManagerImpl configurationManagerImplSpy = Mockito.spy(new ConfigurationManagerImpl());
    @Test
    public void validateIfIntValueIsInRangeTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "3", "1-5");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfIntValueIsInRangeTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "9", "1-5");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestValidValuesReturnNull() {
        String testVariable = "";
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateIfStringValueIsInRangeTestInvalidValuesReturnString() {
        String testVariable = "";
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertEquals("The provided value is not returnMsg.", testVariable);
        }
    }


    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesValidValueReturnNull() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg2").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesInvalidValueReturnMessages() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg2").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg3").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertEquals("The provided value is neither returnMsg1 NOR returnMsg2 NOR returnMsg3.", testVariable);
    }


    @Test
    public void validateRangePrivateIpTestValidValueReturnNull() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(true);
        String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangePrivateIpTestInvalidValueReturnString() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(false);
        String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
        Assert.assertEquals("a valid site local IP address", testVariable);
    }

    @Test
    public void validateRangeHypervisorListTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeHypervisorListTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware,Any,InvalidHypervisorName");
        Assert.assertEquals("a valid hypervisor type", testVariable);
    }

    @Test
    public void validateRangeInstanceNameTestValidValueReturnNull() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(true);
        String testVariable = configurationManagerImplSpy.validateRangeInstanceName("ThisStringShouldBeValid");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeInstanceNameTestInvalidValueReturnString() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(false);
        String testVariable = configurationManagerImplSpy.validateRangeInstanceName("This string should not be valid.");
        Assert.assertEquals("a valid instance name (instance names cannot contain hyphens, spaces or plus signs)", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringShould.Work");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotStartWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeast" +
                "OnePeriodEverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXhNOBNTNAoMCQqJMzcvFSBwHUhmWHftjfTNUaHR");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsNotAValidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueReturnNull() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithAStarAndAPeriod.ThisShouldWorkEvenThoughItIsOverTwoHundredAnd" +
                "ThirtyEight.CharactersLong.BecauseTheFirstTwoCharactersAreIgnored.TheRestOfThisStringWasRandomlyGenerated.MgTUerXPlLyMaUpKTjAhxasFYRCfNCXmtWDwqSDOcTjASWlAXS");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeastOnePeriod" +
                "EverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXNOBNTNAoMChQqJMzcvFSBwHUhmWHftjfTNUaHRKVyXm");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsNotAValidValueReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeOtherTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "SoShouldThis", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeOtherTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "ThisShouldNotWork", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNotNull(testVariable);
    }
}
