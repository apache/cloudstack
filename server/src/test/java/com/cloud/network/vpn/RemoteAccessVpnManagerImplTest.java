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
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.RemoteAccessVpnVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.context.CallContext;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RemoteAccessVpnManagerImplTest extends TestCase {

    private static final long VPC_ID = 12L;
    private static final long PUBLIC_IP_ID = 31L;

    Class<InvalidParameterValueException> expectedException = InvalidParameterValueException.class;
    Class<CloudRuntimeException> cloudRuntimeException = CloudRuntimeException.class;

    private RemoteAccessVPNServiceProvider mockProvider(Network.Provider networkProvider) {
        RemoteAccessVPNServiceProvider provider = Mockito.mock(RemoteAccessVPNServiceProvider.class,
                Mockito.withSettings().extraInterfaces(NetworkElement.class));
        Mockito.when(((NetworkElement) provider).getProvider()).thenReturn(networkProvider);
        return provider;
    }

    private RemoteAccessVpnManagerImpl managerWithProvider(RemoteAccessVPNServiceProvider provider) {
        RemoteAccessVpnManagerImpl manager = new RemoteAccessVpnManagerImpl();
        manager._vpnServiceProviders = List.of(provider);
        manager._networkMgr = Mockito.mock(com.cloud.network.NetworkModel.class);
        manager.vpcManager = Mockito.mock(VpcManager.class);
        return manager;
    }

    @Test
    public void createRemoteAccessVpnRejectsVpcWithoutMappedProviderBeforePersistence() throws NetworkRuleConflictException {
        RemoteAccessVPNServiceProvider provider = mockProvider(Network.Provider.VPCVirtualRouter);
        RemoteAccessVpnManagerImpl manager = managerWithProvider(provider);
        manager._remoteAccessVpnDao = Mockito.mock(RemoteAccessVpnDao.class);
        manager._ipAddressDao = Mockito.mock(IPAddressDao.class);
        manager._accountMgr = Mockito.mock(AccountManager.class);
        manager._vpcDao = Mockito.mock(VpcDao.class);

        PublicIpAddress publicIpAddress = Mockito.mock(PublicIpAddress.class);
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        VpcVO vpc = Mockito.mock(VpcVO.class);
        Account caller = Mockito.mock(Account.class);
        CallContext context = Mockito.mock(CallContext.class);

        Mockito.when(context.getCallingAccount()).thenReturn(caller);
        Mockito.when(manager._networkMgr.getPublicIpAddress(PUBLIC_IP_ID)).thenReturn(publicIpAddress);
        Mockito.when(publicIpAddress.readyToUse()).thenReturn(true);
        Mockito.when(manager._ipAddressDao.acquireInLockTable(PUBLIC_IP_ID)).thenReturn(ipAddress);
        Mockito.when(ipAddress.getAssociatedWithNetworkId()).thenReturn(null);
        Mockito.when(ipAddress.getVpcId()).thenReturn(VPC_ID);
        Mockito.when(ipAddress.isSourceNat()).thenReturn(true);
        Mockito.when(manager._vpcDao.findById(VPC_ID)).thenReturn(vpc);
        Mockito.when(vpc.getId()).thenReturn(VPC_ID);
        Mockito.when(vpc.getCidr()).thenReturn("10.2.0.0/24");
        Mockito.when(manager.vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter)).thenReturn(false);

        try (MockedStatic<CallContext> callContext = Mockito.mockStatic(CallContext.class)) {
            callContext.when(CallContext::current).thenReturn(context);

            InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                    () -> manager.createRemoteAccessVpn(PUBLIC_IP_ID, "10.1.2.1-10.1.2.8", false, null));

            Assert.assertTrue(exception.getMessage().contains("does not implement the Remote Access VPN service"));
        }
        Mockito.verify(manager._remoteAccessVpnDao, Mockito.never()).persist(Mockito.any());
        Mockito.verify(manager.vpcManager, Mockito.never()).configStaticNatForVpcVr(Mockito.any(), Mockito.any());
        Mockito.verify(manager._ipAddressDao).releaseFromLockTable(PUBLIC_IP_ID);
    }

    @Test
    public void startRemoteAccessVpnRejectsLegacyRecordWithoutMappedProvider() throws ResourceUnavailableException {
        RemoteAccessVPNServiceProvider provider = mockProvider(Network.Provider.VPCVirtualRouter);
        RemoteAccessVpnManagerImpl manager = managerWithProvider(provider);
        manager._remoteAccessVpnDao = Mockito.mock(RemoteAccessVpnDao.class);
        manager._accountMgr = Mockito.mock(AccountManager.class);
        manager._firewallMgr = Mockito.mock(FirewallManager.class);
        RemoteAccessVpnVO vpn = Mockito.mock(RemoteAccessVpnVO.class);
        Account caller = Mockito.mock(Account.class);
        CallContext context = Mockito.mock(CallContext.class);
        Mockito.when(vpn.getVpcId()).thenReturn(VPC_ID);
        Mockito.when(manager._remoteAccessVpnDao.findByPublicIpAddress(31L)).thenReturn(vpn);
        Mockito.when(manager.vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter)).thenReturn(false);
        Mockito.when(context.getCallingAccount()).thenReturn(caller);

        try (MockedStatic<CallContext> callContext = Mockito.mockStatic(CallContext.class)) {
            callContext.when(CallContext::current).thenReturn(context);

            InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                    () -> manager.startRemoteAccessVpn(31L, false));

            Assert.assertTrue(exception.getMessage().contains("does not implement the Remote Access VPN service"));
        }
        Mockito.verify(provider, Mockito.never()).startVpn(vpn);
        Mockito.verify(manager._remoteAccessVpnDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void startRemoteAccessVpnFailsWhenMappedProviderDoesNotStartIt() throws ResourceUnavailableException {
        RemoteAccessVPNServiceProvider provider = mockProvider(Network.Provider.VPCVirtualRouter);
        RemoteAccessVpnManagerImpl manager = managerWithProvider(provider);
        manager._remoteAccessVpnDao = Mockito.mock(RemoteAccessVpnDao.class);
        manager._accountMgr = Mockito.mock(AccountManager.class);
        manager._firewallMgr = Mockito.mock(FirewallManager.class);
        RemoteAccessVpnVO vpn = Mockito.mock(RemoteAccessVpnVO.class);
        Account caller = Mockito.mock(Account.class);
        CallContext context = Mockito.mock(CallContext.class);
        Mockito.when(vpn.getId()).thenReturn(22L);
        Mockito.when(vpn.getVpcId()).thenReturn(VPC_ID);
        Mockito.when(manager._remoteAccessVpnDao.findByPublicIpAddress(32L)).thenReturn(vpn);
        Mockito.when(manager.vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter)).thenReturn(true);
        Mockito.when(context.getCallingAccount()).thenReturn(caller);
        Mockito.when(provider.startVpn(vpn)).thenReturn(false);
        Mockito.when(provider.getName()).thenReturn("VpcVirtualRouter");

        try (MockedStatic<CallContext> callContext = Mockito.mockStatic(CallContext.class)) {
            callContext.when(CallContext::current).thenReturn(context);

            ResourceUnavailableException exception = Assert.assertThrows(ResourceUnavailableException.class,
                    () -> manager.startRemoteAccessVpn(32L, false));

            Assert.assertTrue(exception.getMessage().contains("Failed to start Remote Access VPN"));
        }
        Mockito.verify(manager._remoteAccessVpnDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

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

        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.FALSE);
            Mockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.TRUE);

            InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
                new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
            });

            assertEquals(expectedMessage, assertThrows.getMessage());
        }
    }

    @Test
    public void validateValidateIpRangeSecondElementInvalidMustThrowException(){
        String ipRange = "192.168.0.1-192.168.0.400";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("One or both IPs sets in the range [%s] are invalid IPs.", ipRange);

        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {

            Mockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.TRUE);
            Mockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.FALSE);

            InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
                new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
            });

            assertEquals(expectedMessage, assertThrows.getMessage());
        }
    }

    @Test
    public void validateValidateIpRangeBothElementsInvalidMustThrowException(){
        String ipRange = "192.168.0.256-192.168.0.300";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("One or both IPs sets in the range [%s] are invalid IPs.", ipRange);

        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {

            Mockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.FALSE);
            Mockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.FALSE);

            InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
                new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
            });

            assertEquals(expectedMessage, assertThrows.getMessage());
        }
    }

    @Test
    public void validateValidateIpRangeInvalidIpRangeMustThrowException(){
        String ipRange = "192.168.0.255-192.168.0.1";
        String[] range = ipRange.split("-");
        String expectedMessage = String.format("Range of IPs [%s] is invalid.", ipRange);

        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {

            Mockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.TRUE);
            Mockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.TRUE);
            Mockito.when(NetUtils.validIpRange(range[0], range[1])).thenReturn(Boolean.FALSE);

            InvalidParameterValueException assertThrows = Assert.assertThrows(expectedMessage, expectedException, () -> {
                new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
            });

            assertEquals(expectedMessage, assertThrows.getMessage());
        }
    }

    @Test
    public void validateValidateIpRangeValidIpRangeMustValidate(){
        String ipRange = "192.168.0.1-192.168.0.255";
        String[] range = ipRange.split("-");

        try (MockedStatic<NetUtils> ignored = Mockito.mockStatic(NetUtils.class)) {
            Mockito.when(NetUtils.isValidIp4(range[0])).thenReturn(Boolean.TRUE);
            Mockito.when(NetUtils.isValidIp4(range[1])).thenReturn(Boolean.TRUE);
            Mockito.when(NetUtils.validIpRange(range[0], range[1])).thenReturn(Boolean.TRUE);

            new RemoteAccessVpnManagerImpl().validateIpRange(ipRange, expectedException);
        }
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
