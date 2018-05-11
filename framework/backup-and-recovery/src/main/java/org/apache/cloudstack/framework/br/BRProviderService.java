package org.apache.cloudstack.framework.br;

import java.util.List;

/**
 * Backup and Recovery Provider Services
 */
public interface BRProviderService {

    /**
     * Add a new Backup and Recovery provider
     */
    BRProvider addBRProvider(String name, String url, String username, String password, Long zoneId, String providerName);

    /**
     * List existing Backup and Recovery providers
     */
    List<BRProvider> listBRProviders();

    /**
     * Delete existing Backup and Recovery provider
     */
    boolean deleteBRProvider(String providerId);
}
