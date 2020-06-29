package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.google.gson.Gson;
import org.apache.cloudstack.api.net.NetworkPrerequisiteTO;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DownloadAnswerTest {
    Gson gson = GsonHelper.getGson();

    VMTemplateStorageResourceAssoc.Status status = VMTemplateStorageResourceAssoc.Status.DOWNLOADED;
    DownloadAnswer answer = new DownloadAnswer("nothin wrong", status);

    @Test
    public void redeserialise ()
    {
        String json = gson.toJson(answer);
        DownloadAnswer received = gson.fromJson(json, DownloadAnswer.class);
        Assert.assertEquals(received,answer);
    }
    @Test
    public void properties ()
    {
        List<OVFPropertyTO> properties = new ArrayList<>();
        properties.add(new OVFPropertyTO());
        List<NetworkPrerequisiteTO> networks = new ArrayList<>();
        networks.add(new NetworkPrerequisiteTO());

        answer.setOvfProperties(properties);
        answer.setNetworkRequirements(networks);

        String json = gson.toJson(answer);
        Answer received = gson.fromJson(json, Answer.class);
        Assert.assertEquals(received,answer);
    }
}