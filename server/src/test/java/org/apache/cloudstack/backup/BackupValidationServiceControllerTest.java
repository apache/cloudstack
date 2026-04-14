package org.apache.cloudstack.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.backup.dao.InternalBackupServiceJobDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class BackupValidationServiceControllerTest {

    @Mock
    private DataCenterDao dataCenterDaoMock;

    @Mock
    private DataCenterVO dataCenterVoMock;

    @Mock
    private HostVO hostVO;

    @Mock
    private InternalBackupServiceJobDao internalBackupServiceJobDaoMock;

    @Mock
    private InternalBackupServiceJobVO internalBackupServiceJobVoMock;

    @Mock
    private ConfigKey<Boolean> backupValidationTaskEnabledMock;

    @Spy
    @InjectMocks
    private BackupValidationServiceController backupValidationServiceControllerSpy;

    long datacenterId = 1L;

    @Test
    public void searchAndDispatchJobsTestLockFailureReturnsEarly() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(false).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);

        backupValidationServiceControllerSpy.searchAndDispatchJobs();

        verify(internalBackupServiceJobDaoMock).unlockFromLockTable(any());
        verify(backupValidationServiceControllerSpy, never()).rescheduleLostJobs();
        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
    }

    @Test
    public void searchAndDispatchJobsTestTaskDisabledReturnsAfterReschedule() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceControllerSpy).rescheduleLostJobs();
        doReturn(false).when(backupValidationTaskEnabledMock).value();

        backupValidationServiceControllerSpy.searchAndDispatchJobs();

        verify(backupValidationServiceControllerSpy).rescheduleLostJobs();
        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }

    @Test
    public void searchAndDispatchJobsTestZoneWithoutBackupFrameworkIsSkipped() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupValidationTaskEnabledMock).value();
        doReturn(false).when(backupValidationServiceControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);

        backupValidationServiceControllerSpy.searchAndDispatchJobs();

        verify(internalBackupServiceJobDaoMock, never()).listWaitingJobsAndScheduledToBeforeNow(anyLong(), any());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }

    @Test
    public void searchAndDispatchJobsTestEmptyWaitingJobsSkipsDispatch() {
        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupValidationTaskEnabledMock).value();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(true).when(backupValidationServiceControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);

        backupValidationServiceControllerSpy.searchAndDispatchJobs();

        verify(backupValidationServiceControllerSpy, never()).getHostToNumberOfExecutingJobsAndTotalExecutingJobs(any(), any());
        verify(backupValidationServiceControllerSpy, never()).submitQueuedJobsForExecution(any(), any(), any(), any(), anyLong());
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }

    @Test
    public void searchAndDispatchJobsTestHappyPathDispatchesJobs() {
        Pair<HashMap<HostVO, Long>, Integer> executingJobsPair = new Pair<>(new HashMap<>(), 0);

        doReturn(List.of(dataCenterVoMock)).when(dataCenterDaoMock).listEnabledZones();
        doReturn(true).when(internalBackupServiceJobDaoMock).lockInLockTable("validation_lock", 300);
        doNothing().when(backupValidationServiceControllerSpy).rescheduleLostJobs();
        doReturn(true).when(backupValidationTaskEnabledMock).value();
        doReturn(datacenterId).when(dataCenterVoMock).getId();
        doReturn(true).when(backupValidationServiceControllerSpy).isFrameworkEnabledForZone(dataCenterVoMock);
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupValidationServiceControllerSpy).filterJobsOfDomainsAndAccountsWithDisabledValidationTask(any());
        doReturn(executingJobsPair).when(backupValidationServiceControllerSpy).getHostToNumberOfExecutingJobsAndTotalExecutingJobs(eq(dataCenterVoMock), any());
        doReturn(List.of(new Pair<>(hostVO, 0L))).when(backupValidationServiceControllerSpy).filterHostsWithTooManyJobs(any(), any());
        doReturn(List.of(internalBackupServiceJobVoMock)).when(backupValidationServiceControllerSpy).thinJobsToStartList(eq(dataCenterVoMock), any(), anyInt(), any());
        doNothing().when(backupValidationServiceControllerSpy).submitQueuedJobsForExecution(any(), any(), any(), any(), eq(datacenterId));

        backupValidationServiceControllerSpy.searchAndDispatchJobs();


        verify(backupValidationServiceControllerSpy).submitQueuedJobsForExecution(any(), any(), any(), any(), eq(datacenterId));
        verify(internalBackupServiceJobDaoMock).unlockFromLockTable("validation_lock");
    }
}
