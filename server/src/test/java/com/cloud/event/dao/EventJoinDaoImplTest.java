package com.cloud.event.dao;

import java.util.UUID;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.EventResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentContext.class)
public class EventJoinDaoImplTest {

    @Mock
    protected EntityManager entityManager;

    @InjectMocks
    private EventJoinDaoImpl dao = new EventJoinDaoImpl();

    @Test
    public void testNewEventViewResource() {
        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        final String resourceName = "TestVM";
        EventJoinVO event = Mockito.mock(EventJoinVO.class);
        Mockito.when(event.getResourceId()).thenReturn(resourceId);
        Mockito.when(event.getResourceType()).thenReturn(resourceType);
        Mockito.when(event.getResourceType()).thenReturn(resourceType);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getUuid()).thenReturn(resourceUuid);
        Mockito.when(vm.getName()).thenReturn(resourceName);
        Mockito.when(entityManager.findByIdIncludingRemoved(VirtualMachine.class, resourceId)).thenReturn(vm);
        EventResponse response = dao.newEventResponse(event);
        Assert.assertEquals(response.getResourceId(), resourceUuid);
        Assert.assertEquals(response.getResourceType(), resourceType);
        Assert.assertEquals(response.getResourceName(), resourceName);
    }
}
