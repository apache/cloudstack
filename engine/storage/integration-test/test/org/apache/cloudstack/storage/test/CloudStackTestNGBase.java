package org.apache.cloudstack.storage.test;

import java.lang.reflect.Method;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;

public class CloudStackTestNGBase extends AbstractTestNGSpringContextTests {
    private String hostGateway;
    private String hostCidr;
    private String hostIp;
    private String hostGuid;
    private String templateUrl;
    private String localStorageUuid;
    private Transaction txn;
    
    @BeforeMethod(alwaysRun = true)
    protected  void injectDB(Method testMethod) throws Exception {
        txn = Transaction.open(testMethod.getName());
    }
    
    @AfterMethod(alwaysRun = true)
    protected void closeDB(Method testMethod) throws Exception {
        if (txn != null) {
            txn.close();
        }
    }
    
    @BeforeMethod(alwaysRun = true)
    @Parameters({"devcloud-host-uuid", "devcloud-host-gateway", "devcloud-host-cidr", "devcloud-host-ip", "template-url", "devcloud-local-storage-uuid"})
    protected void setup(String hostuuid, String gateway, String cidr, String hostIp, String templateUrl, String localStorageUuid) {
        this.hostGuid = hostuuid;
        this.hostGateway = gateway;
        this.hostCidr = cidr;
        this.hostIp = hostIp;
        this.templateUrl = templateUrl;
        this.localStorageUuid = localStorageUuid;
    }
    
    protected String getHostGuid() {
        return this.hostGuid;
    }
    
    protected String getHostGateway() {
        return this.hostGateway;
    }
    
    protected String getHostCidr() {
        return this.hostCidr;
    }
    
    protected String getHostIp() {
        return this.hostIp;
    }
    
    protected String getTemplateUrl() {
        return this.templateUrl;
    }
    
    protected String getLocalStorageUuid() {
        return this.localStorageUuid;
    }
}
