package org.apache.cloudstack.api.command.user.volume;

import com.cloud.exception.InvalidParameterValueException;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CheckAndRepairVolumeCmdTest extends TestCase {
    private CheckAndRepairVolumeCmd checkAndRepairVolumeCmd;
    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        checkAndRepairVolumeCmd = new CheckAndRepairVolumeCmd();
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetRepair() {
        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", "all");
        assertEquals("all", checkAndRepairVolumeCmd.getRepair());

        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", "LEAKS");
        assertEquals("leaks", checkAndRepairVolumeCmd.getRepair());

        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", null);
        assertNull(checkAndRepairVolumeCmd.getRepair());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetRepairInvalid() {
        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", "RANDOM STRING");
        checkAndRepairVolumeCmd.getRepair();
    }
}
