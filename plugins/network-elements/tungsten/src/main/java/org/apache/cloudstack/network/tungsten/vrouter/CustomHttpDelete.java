package org.apache.cloudstack.network.tungsten.vrouter;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class CustomHttpDelete extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";

    public CustomHttpDelete() {
    }

    public CustomHttpDelete(URI uri) {
        this.setURI(uri);
    }

    public CustomHttpDelete(String uri) {
        this.setURI(URI.create(uri));
    }

    public String getMethod() {
        return METHOD_NAME;
    }
}
