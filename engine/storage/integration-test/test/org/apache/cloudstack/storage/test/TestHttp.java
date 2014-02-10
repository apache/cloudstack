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
package org.apache.cloudstack.storage.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.Assert;

import org.apache.commons.httpclient.HttpException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Parameters;

@ContextConfiguration(locations = "classpath:/storageContext.xml")
public class TestHttp extends AbstractTestNGSpringContextTests {
    @Test
    @Parameters("template-url")
    public void testHttpclient(String templateUrl) {
        HttpHead method = new HttpHead(templateUrl);
        DefaultHttpClient client = new DefaultHttpClient();

        OutputStream output = null;
        long length = 0;
        try {
            HttpResponse response = client.execute(method);
            length = Long.parseLong(response.getFirstHeader("Content-Length").getValue());
            System.out.println(response.getFirstHeader("Content-Length").getValue());
            File localFile = new File("/tmp/test");
            if (!localFile.exists()) {
                localFile.createNewFile();
            }

            HttpGet getMethod = new HttpGet(templateUrl);
            response = client.execute(getMethod);
            HttpEntity entity = response.getEntity();

            output = new BufferedOutputStream(new FileOutputStream(localFile));
            entity.writeTo(output);
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        File f = new File("/tmp/test");
        Assert.assertEquals(f.length(), length);
    }
}
