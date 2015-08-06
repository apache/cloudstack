package com.cloud.network.element;

import com.cloud.network.Network;

/**
 * Created by bharat on 11/08/15.
 */
public interface RedundantResource {
     public void configureResource(Network network);
     public int getResourceCount(Network network);
}
