package com.cloud.ucs.manager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.cloud.utils.exception.CloudRuntimeException;

public class UcsHttpClient {
    private static HttpClient client = new HttpClient();
    private String url;

    public UcsHttpClient(String ip) {
        this.url = String.format("http://%s/nuova", ip);
    }

    public String call(String xml) {
        PostMethod post = new PostMethod(url);
        post.setRequestEntity(new StringRequestEntity(xml));
        post.setRequestHeader("Content-type", "text/xml");
        try {
            int result = client.executeMethod(post);
            if (result != 200) {
               throw new CloudRuntimeException("Call failed: " + post.getResponseBodyAsString()); 
            }
            return post.getResponseBodyAsString();
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
    }
}
