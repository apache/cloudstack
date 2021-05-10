/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.network.vpn;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import java.lang.reflect.InvocationTargetException;
import javax.naming.ConfigurationException;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NetUtils.class)
public class RemoteAccessVpnManagerImplTest extends TestCase {

    Class<InvalidParameterValueException> expectedException = InvalidParameterValueException.class;
    Class<CloudRuntimeException> cloudRuntimeException = CloudRuntimeException.class;

    @Test
    public void validateValidateIpRangeRangeLengthLessThan2MustThrowException(){
        String ipRange = "192.168.0.1";
        String expectedMessage = String.format("IP range [%s] is an invalid IP range.", ipRange);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateValidateIpRangeRangeLengthHigherThan2MustThrowException(){
        String ipRange = "192.168.0.1-192.168.0.31-192.168.0.63";
        String expectedMessage = String.format("IP range [%s] is an invalid IP range.", ipRange);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateValidateIpRangeFirstElementInvalidMustThrowException(){
        String ipRange = "192.168.0.400-192.168.0.255";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("One or both IPs sets in the range [%s] are invalid IPs.", ipRange);

        PowerMockito.mockStatic(NetUtils.class);

        PowerMockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.FALSE);
        PowerMockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.TRUE);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateValidateIpRangeSecondElementInvalidMustThrowException(){
        String ipRange = "192.168.0.1-192.168.0.400";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("One or both IPs sets in the range [%s] are invalid IPs.", ipRange);

        PowerMockito.mockStatic(NetUtils.class);

        PowerMockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.TRUE);
        PowerMockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.FALSE);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateValidateIpRangeBothElementsInvalidMustThrowException(){
        String ipRange = "192.168.0.256-192.168.0.300";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("One or both IPs sets in the range [%s] are invalid IPs.", ipRange);

        PowerMockito.mockStatic(NetUtils.class);

        PowerMockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.FALSE);
        PowerMockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.FALSE);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateValidateIpRangeInvalidIpRangeMustThrowException(){
        String ipRange = "192.168.0.255-192.168.0.1";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("Range of IPs [%s] is invalid.", ipRange);

        PowerMockito.mockStatic(NetUtils.class);

        PowerMockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.TRUE);
        PowerMockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.TRUE);
        PowerMockito.when(NetUtils.validIpRange(range[0], range[1])).thenReturn(Boolean.FALSE);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateValidateIpRangeValidIpRangeMustValidate(){
        String ipRange = "192.168.0.1-192.168.0.255";
        String[] range = ipRange.split("-");

        PowerMockito.mockStatic(NetUtils.class);

        PowerMockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.TRUE);
        PowerMockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.TRUE);
        PowerMockito.when(NetUtils.validIpRange(range[0], range[1])).thenReturn(Boolean.TRUE);

        new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
    }

    private <T extends Throwable> void handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(Class<T> exceptionToCatch){
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exceptionToCatch, "Test");
    }

    private <T extends Throwable> void handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(Class<T> exceptionToCatch, String exceptionMessage){
        String errorMessage = "Test";
        String expectedMessage = String.format("Unexpected exception [%s] while throwing error [%s] on validateIpRange.", exceptionMessage, errorMessage);

        CloudRuntimeException assertThrows = Assert.assertThrows(expectedMessage, cloudRuntimeException, () -> {
            new RemoteAccessVpnManagerImpl().handleExceptionOnValidateIpRangeError(exceptionToCatch, errorMessage);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenNoSuchMethodExceptionThrowCloudRuntimeException(){
        Class<NoSuchMethodException> exception = NoSuchMethodException.class;
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exception);
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenSecurityExceptionThrowCloudRuntimeException(){
        Class<SecurityException> exception = SecurityException.class;
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exception);
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenInstantiationExceptionThrowCloudRuntimeException(){
        Class<InstantiationException> exception = InstantiationException.class;
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exception);
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenIllegalAccessExceptionThrowCloudRuntimeException(){
        Class<IllegalAccessException> exception = IllegalAccessException.class;
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exception);
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenIllegalArgumentExceptionThrowCloudRuntimeException(){
        Class<IllegalArgumentException> exception = IllegalArgumentException.class;
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exception);
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenInvocationTargetExceptionThrowCloudRuntimeException(){
        Class<InvocationTargetException> exception = InvocationTargetException.class;
        handleExceptionOnValidateIpRangeErrorMustThrowCloudRuntimeException(exception, "java.lang.reflect.InvocationTargetException.<init>(java.lang.String)");
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenConfigurationExceptionThrowConfigurationException(){
        Class<ConfigurationException> exception = ConfigurationException.class;
        String expectedMessage = "Test";

        ConfigurationException assertThrows = Assert.assertThrows(expectedMessage, exception, () -> {
            new RemoteAccessVpnManagerImpl().handleExceptionOnValidateIpRangeError(exception, expectedMessage);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateHandleExceptionOnValidateIpRangeErrorWhenInvalidParameterValueExceptionThrowInvalidParameterValueException(){
        Class<InvalidParameterValueException> exception = InvalidParameterValueException.class;
        String expectedMessage = "Test";

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, exception, () -> {
            new RemoteAccessVpnManagerImpl().handleExceptionOnValidateIpRangeError(exception, expectedMessage);
        });

        assertEquals(expectedMessage, assertThrows.getMessage());
    }
}
