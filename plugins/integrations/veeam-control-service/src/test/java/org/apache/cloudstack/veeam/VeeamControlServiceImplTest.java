package org.apache.cloudstack.veeam;

import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(MockitoJUnitRunner.class)
public class VeeamControlServiceImplTest {

    @Test
    public void test_parseImageTransfer() {
        String data = "{\"active\":false,\"direction\":\"upload\",\"format\":\"cow\",\"inactivity_timeout\":3600,\"phase\":\"cancelled\",\"shallow\":false,\"transferred\":0,\"link\":[],\"disk\":{\"id\":\"dba4d72d-01de-4267-aa8e-305996b53599\"},\"image\":{},\"backup\":{\"creation_date\":0}}";
        Mapper mapper = new Mapper();
        try {
            ImageTransfer request = mapper.jsonMapper().readValue(data, ImageTransfer.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
