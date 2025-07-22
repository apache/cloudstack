package com.cloud.gpu.dao;

import com.cloud.gpu.GpuCardVO;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GpuCardDaoImplTest {

    @Spy
    @InjectMocks
    GpuCardDaoImpl gpuCardDaoImpl = new GpuCardDaoImpl();

    @Test
    public void findByVendorIdAndDeviceId() {
        doReturn(mock(GpuCardVO.class)).when(gpuCardDaoImpl).findOneBy(any(SearchCriteria.class));

        GpuCardVO gpuCard = gpuCardDaoImpl.findByVendorIdAndDeviceId("0d1a", "1a3b");
        Assert.assertNotNull("Expected non-null gpu card", gpuCard);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuCardDaoImpl).findOneBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause",
                "gpu_card.vendor_id = ?  AND gpu_card.device_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }
}
