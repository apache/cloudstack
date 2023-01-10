/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.resource;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Ignore
public class LocalNfsSecondaryStorageResourceTest extends TestCase {
    private static Map<String, Object> testParams;


    LocalNfsSecondaryStorageResource resource;

    @Before
    @Override
    public void setUp() throws ConfigurationException {
        resource = new LocalNfsSecondaryStorageResource();
        resource.setInSystemVM(true);

        testParams = PropertiesUtil.toMap(loadProperties());
        resource.configureStorageLayerClass(testParams);
        Object testLocalRoot = testParams.get("testLocalRoot");
        resource.setParentPath("/mnt");

        if (testLocalRoot != null) {
            resource.setParentPath((String)testLocalRoot);
        }

        System.setProperty("paths.script", "/Users/edison/develop/asf-master/script");
        //resource.configure("test", new HashMap<String, Object>());
    }

    @Test
    public void testExecuteRequest() throws Exception {
        TemplateObjectTO template = Mockito.mock(TemplateObjectTO.class);
        NfsTO cacheStore = Mockito.mock(NfsTO.class);
        Mockito.when(cacheStore.getUrl()).thenReturn("nfs://nfs2.lab.vmops.com/export/home/edison/");
        SwiftTO swift = Mockito.mock(SwiftTO.class);
        Mockito.when(swift.getEndPoint()).thenReturn("https://objects.dreamhost.com/auth");
        Mockito.when(swift.getAccount()).thenReturn("cloudstack");
        Mockito.when(swift.getUserName()).thenReturn("images");
        Mockito.when(swift.getKey()).thenReturn("oxvELQaOD1U5_VyosGfA-wpZ7uBWEff-CUBGCM0u");

        Mockito.when(template.getDataStore()).thenReturn(swift);
        Mockito.when(template.getPath()).thenReturn("template/1/1/");
        Mockito.when(template.isRequiresHvm()).thenReturn(true);
        Mockito.when(template.getId()).thenReturn(1L);
        Mockito.when(template.getFormat()).thenReturn(Storage.ImageFormat.VHD);
        Mockito.when(template.getOrigUrl()).thenReturn("http://nfs1.lab.vmops.com/templates/test.bz2");
        Mockito.when(template.getName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(template.getObjectType()).thenReturn(DataObjectType.TEMPLATE);

        DownloadCommand cmd = new DownloadCommand(template, 100000L);
        cmd.setCacheStore(cacheStore);
        DownloadAnswer answer = (DownloadAnswer)resource.executeRequest(cmd);
        Assert.assertTrue(answer.getResult());

        Mockito.when(template.getPath()).thenReturn(answer.getInstallPath());
        Mockito.when(template.getDataStore()).thenReturn(swift);
        //download swift:
        Mockito.when(cacheStore.getRole()).thenReturn(DataStoreRole.ImageCache);
        TemplateObjectTO destTemplate = Mockito.mock(TemplateObjectTO.class);
        Mockito.when(destTemplate.getPath()).thenReturn("template/1/2");
        Mockito.when(destTemplate.getDataStore()).thenReturn(cacheStore);
        Mockito.when(destTemplate.getObjectType()).thenReturn(DataObjectType.TEMPLATE);
        CopyCommand cpyCmd = new CopyCommand(template, destTemplate, 10000, true);
        CopyCmdAnswer copyCmdAnswer = (CopyCmdAnswer)resource.executeRequest(cpyCmd);
        Assert.assertTrue(copyCmdAnswer.getResult());

        //list template
        ListTemplateCommand listCmd = new ListTemplateCommand(swift);
        ListTemplateAnswer listAnswer = (ListTemplateAnswer)resource.executeRequest(listCmd);

        Assert.assertTrue(listAnswer.getTemplateInfo().size() > 0);
    }

    public static Properties loadProperties() throws ConfigurationException {
        Properties properties = new Properties();
        final File file = PropertiesUtil.findConfigFile("agent.properties");
        if (file == null) {
            throw new ConfigurationException("Unable to find agent.properties.");
        }


        try(FileInputStream fs = new FileInputStream(file);) {
            properties.load(fs);
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: " + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading " + file.getAbsolutePath(), ex);
        }
        return properties;
    }
}
