package com.cloud.kubernetesversion;

import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.api.command.admin.kubernetesversion.AddKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetesversion.DeleteKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.kubernetesversion.ListKubernetesSupportedVersionsCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.kubernetescluster.KubernetesClusterService;
import com.cloud.kubernetescluster.KubernetesClusterVO;
import com.cloud.kubernetescluster.dao.KubernetesClusterDao;
import com.cloud.kubernetesversion.dao.KubernetesSupportedVersionDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ComponentContext.class})
public class KubernetesVersionServiceTest {

    @InjectMocks
    private KubernetesVersionService kubernetesVersionService = new KubernetesVersionManagerImpl();

    @Mock
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Mock
    private KubernetesClusterDao kubernetesClusterDao;
    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private TemplateJoinDao templateJoinDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private TemplateApiService templateService;

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        overrideDefaultConfigValue(KubernetesClusterService.KubernetesServiceEnabled, "_defaultValue", "true");

        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(1L);
        when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);

        TemplateJoinVO templateJoinVO = Mockito.mock(TemplateJoinVO.class);
        when(templateJoinVO.getId()).thenReturn(1L);
        when(templateJoinVO.getUrl()).thenReturn("https://download.cloudstack.com");
        when(templateJoinVO.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Ready);
        when(templateJoinDao.findById(Mockito.anyLong())).thenReturn(templateJoinVO);

        KubernetesSupportedVersionVO versionVO = Mockito.mock(KubernetesSupportedVersionVO.class);
        when(versionVO.getSemanticVersion()).thenReturn(KubernetesVersionService.MIN_KUBERNETES_VERSION);
        when(kubernetesSupportedVersionDao.persist(Mockito.any(KubernetesSupportedVersionVO.class))).thenReturn(versionVO);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void listKubernetesSupportedVersionsTest() {
        ListKubernetesSupportedVersionsCmd cmd = Mockito.mock(ListKubernetesSupportedVersionsCmd.class);
        List<KubernetesSupportedVersionVO> versionVOs = new ArrayList<>();
        KubernetesSupportedVersionVO versionVO = Mockito.mock(KubernetesSupportedVersionVO.class);
        when(versionVO.getSemanticVersion()).thenReturn(KubernetesVersionService.MIN_KUBERNETES_VERSION);
        versionVOs.add(versionVO);
        when(kubernetesSupportedVersionDao.listAll()).thenReturn(versionVOs);
        when(kubernetesSupportedVersionDao.listAllInZone(Mockito.anyLong())).thenReturn(versionVOs);
        when(kubernetesSupportedVersionDao.findById(Mockito.anyLong())).thenReturn(versionVO);
        kubernetesVersionService.listKubernetesSupportedVersions(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addKubernetesSupportedVersionLowerUnsupportedTest() {
        AddKubernetesSupportedVersionCmd cmd = Mockito.mock(AddKubernetesSupportedVersionCmd.class);
        AccountVO account = new AccountVO("admin", 1L, "", Account.ACCOUNT_TYPE_ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(cmd.getSemanticVersion()).thenReturn("1.1.1");
        kubernetesVersionService.addKubernetesSupportedVersion(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addKubernetesSupportedVersionIsoIdUrlTest() {
        AddKubernetesSupportedVersionCmd cmd = Mockito.mock(AddKubernetesSupportedVersionCmd.class);
        AccountVO account = new AccountVO("admin", 1L, "", Account.ACCOUNT_TYPE_ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        when(cmd.getSemanticVersion()).thenReturn(KubernetesVersionService.MIN_KUBERNETES_VERSION);
        CallContext.register(user, account);
        when(cmd.getIsoId()).thenReturn(1L);
        when(cmd.getUrl()).thenReturn("url");
        kubernetesVersionService.addKubernetesSupportedVersion(cmd);
    }

    @Test
    public void addKubernetesSupportedVersionIsoIdTest() {
        AddKubernetesSupportedVersionCmd cmd = Mockito.mock(AddKubernetesSupportedVersionCmd.class);
        AccountVO account = new AccountVO("admin", 1L, "", Account.ACCOUNT_TYPE_ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(cmd.getSemanticVersion()).thenReturn(KubernetesVersionService.MIN_KUBERNETES_VERSION);
        when(cmd.getIsoId()).thenReturn(1L);
        when(cmd.getUrl()).thenReturn(null);
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        when(templateVO.getId()).thenReturn(1L);
        when(templateVO.getFormat()).thenReturn(Storage.ImageFormat.ISO);
        when(templateVO.isPublicTemplate()).thenReturn(true);
        when(templateVO.isCrossZones()).thenReturn(true);
        when(templateDao.findById(Mockito.anyLong())).thenReturn(templateVO);
        kubernetesVersionService.addKubernetesSupportedVersion(cmd);
    }

    @Test
    public void addKubernetesSupportedVersionIsoUrlTest() throws ResourceAllocationException, NoSuchFieldException {
        AddKubernetesSupportedVersionCmd cmd = Mockito.mock(AddKubernetesSupportedVersionCmd.class);
        AccountVO account = new AccountVO("admin", 1L, "", Account.ACCOUNT_TYPE_ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(cmd.getSemanticVersion()).thenReturn(KubernetesVersionService.MIN_KUBERNETES_VERSION);
        when(cmd.getIsoId()).thenReturn(null);
        when(cmd.getUrl()).thenReturn("https://download.cloudstack.com");
        when(cmd.getChecksum()).thenReturn(null);
        PowerMockito.mockStatic(ComponentContext.class);
        when(ComponentContext.inject(Mockito.any(RegisterIsoCmd.class))).thenReturn(new RegisterIsoCmd());
        when(templateService.registerIso(Mockito.any(RegisterIsoCmd.class))).thenReturn(Mockito.mock(VirtualMachineTemplate.class));
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        when(templateVO.getId()).thenReturn(1L);
        when(templateDao.findById(Mockito.anyLong())).thenReturn(templateVO);
        kubernetesVersionService.addKubernetesSupportedVersion(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void deleteKubernetesSupportedVersionExistingClustersTest() {
        DeleteKubernetesSupportedVersionCmd cmd = Mockito.mock(DeleteKubernetesSupportedVersionCmd.class);
        AccountVO account = new AccountVO("admin", 1L, "", Account.ACCOUNT_TYPE_ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(cmd.isDeleteIso()).thenReturn(true);
        when(kubernetesSupportedVersionDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(KubernetesSupportedVersionVO.class));
        List<KubernetesClusterVO> clusters = new ArrayList<>();
        clusters.add(Mockito.mock(KubernetesClusterVO.class));
        when(kubernetesClusterDao.listAllByKubernetesVersion(Mockito.anyLong())).thenReturn(clusters);
        kubernetesVersionService.deleteKubernetesSupportedVersion(cmd);
    }

    @Test
    public void deleteKubernetesSupportedVersionTest() {
        DeleteKubernetesSupportedVersionCmd cmd = Mockito.mock(DeleteKubernetesSupportedVersionCmd.class);
        AccountVO account = new AccountVO("admin", 1L, "", Account.ACCOUNT_TYPE_ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(cmd.isDeleteIso()).thenReturn(true);
        when(kubernetesSupportedVersionDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(KubernetesSupportedVersionVO.class));
        List<KubernetesClusterVO> clusters = new ArrayList<>();
        when(kubernetesClusterDao.listAllByKubernetesVersion(Mockito.anyLong())).thenReturn(clusters);
        when(templateDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(VMTemplateVO.class));
        PowerMockito.mockStatic(ComponentContext.class);
        when(ComponentContext.inject(Mockito.any(DeleteIsoCmd.class))).thenReturn(new DeleteIsoCmd());
        when(templateService.deleteIso(Mockito.any(DeleteIsoCmd.class))).thenReturn(true);
        when(kubernetesClusterDao.remove(Mockito.anyLong())).thenReturn(true);
        kubernetesVersionService.deleteKubernetesSupportedVersion(cmd);
    }
}