package com.cloud.api;

public interface ResponseObject {
    /**
     * Get the name of the API response
     * @return the name of the API response
     */
    String getResponseName();

    /**
     * Set the name of the API response
     * @param name
     */
    void setResponseName(String name);
}
