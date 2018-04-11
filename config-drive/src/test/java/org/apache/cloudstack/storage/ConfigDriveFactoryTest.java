package org.apache.cloudstack.storage;

import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.DataStoreRole;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ConfigDriveFactoryTest {
    private final static String CONFIGDRIVEFILENAME = "configdrive.iso";
    private final static String CONFIGDRIVEDIR = "ConfigDrive";

    @Mock
    StorageAttacher storageAttacher;

    @Test
    public void executeRequest() {
        List<String[]> vmData = null;
        String label = null;
        DataStoreTO destStore = new DataStoreTO() {
            @Override public DataStoreRole getRole() {
                return DataStoreRole.Primary;
            }

            @Override public String getUuid() {
                return "0123456789abcdef";
            }

            @Override public String getUrl() {
                return "";
            }

            @Override public String getPathSeparator() {
                return "/";
            }
        };
        String isoFile = "/" + CONFIGDRIVEDIR + "/" + "bla" + "/" + CONFIGDRIVEFILENAME;
        boolean create = false;
        boolean update = false;
        HandleConfigDriveIsoCommand cmd = new HandleConfigDriveIsoCommand(null,
                null, destStore, isoFile, false, false);
        ConfigDriveFactory configDriveFactory = new ConfigDriveFactory(1,storageAttacher);
        configDriveFactory.executeRequest(cmd);
    }
}