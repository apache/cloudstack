package org.apache.cloudstack.backup.networker;


import java.util.List;

public interface NetworkerObject {
    String getUuid();

    String getName();

    String getHref();

    String getType();

    List<NetworkerObject> getLinks();
}
