package org.apache.cloudstack.api.command.user.gpu;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ListVgpuProfilesCmdTest {

    @Test
    public void getId() {
        ListVgpuProfilesCmd cmd = new ListVgpuProfilesCmd();
        Assert.assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(id, cmd.getId());
    }

    @Test
    public void getName() {
        ListVgpuProfilesCmd cmd = new ListVgpuProfilesCmd();
        Assert.assertNull(cmd.getName());
        String name = "Test VGPU Profile";
        ReflectionTestUtils.setField(cmd, "name", name);
        Assert.assertEquals(name, cmd.getName());
    }

    @Test
    public void getCardId() {
        ListVgpuProfilesCmd cmd = new ListVgpuProfilesCmd();
        Assert.assertNull(cmd.getCardId());
        Long cardId = 1L;
        ReflectionTestUtils.setField(cmd, "cardId", cardId);
        Assert.assertEquals(cardId, cmd.getCardId());
    }

    @Test
    public void getActiveOnly() {
        ListVgpuProfilesCmd cmd = new ListVgpuProfilesCmd();
        Assert.assertFalse(cmd.getActiveOnly());
        Boolean activeOnly = true;
        ReflectionTestUtils.setField(cmd, "activeOnly", activeOnly);
        Assert.assertEquals(activeOnly, cmd.getActiveOnly());
    }
}
