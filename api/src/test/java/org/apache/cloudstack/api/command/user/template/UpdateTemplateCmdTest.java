package org.apache.cloudstack.api.command.user.template;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class UpdateTemplateCmdTest {

    @Test
    public void testGetTemplateType() {
        UpdateTemplateCmd cmd = new UpdateTemplateCmd();
        ReflectionTestUtils.setField(cmd, "templateType", null);
        Assert.assertNull(cmd.getTemplateType());
        String type = Storage.TemplateType.ROUTING.toString();
        ReflectionTestUtils.setField(cmd, "templateTag", type);
        Assert.assertEquals(type, cmd.getTemplateTag());
    }

    @Test
    public void testGetTemplateTag() {
        UpdateTemplateCmd cmd = new UpdateTemplateCmd();
        ReflectionTestUtils.setField(cmd, "templateTag", null);
        Assert.assertNull(cmd.getTemplateTag());
        String tag = "ABC";
        ReflectionTestUtils.setField(cmd, "templateTag", tag);
        Assert.assertEquals(tag, cmd.getTemplateTag());
    }
}