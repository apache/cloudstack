package com.cloud.gpu.dao;

import com.cloud.gpu.VgpuProfileVO;
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
public class VgpuProfileDaoImplTest {

    @Spy
    @InjectMocks
    VgpuProfileDaoImpl vgpuProfileDaoImpl = new VgpuProfileDaoImpl();

    @Test
    public void findByNameAndCardId() {
        doReturn(mock(VgpuProfileVO.class)).when(vgpuProfileDaoImpl).findOneBy(any(SearchCriteria.class));

        VgpuProfileVO vgpuProfile = vgpuProfileDaoImpl.findByNameAndCardId("test-profile", 1L);
        Assert.assertNotNull("Expected non-null vgpu profile", vgpuProfile);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(vgpuProfileDaoImpl).findOneBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause",
                "vgpu_profile.name = ?  AND vgpu_profile.card_id=?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void removeByCardId() {
        doReturn(1).when(vgpuProfileDaoImpl).remove(any(SearchCriteria.class));

        int removed = vgpuProfileDaoImpl.removeByCardId(123L);
        Assert.assertEquals("Expected one vgpu profile removed", 1, removed);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(vgpuProfileDaoImpl).remove(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "vgpu_profile.card_id=?",
                scCaptor.getValue().getWhereClause().trim());
    }
}
