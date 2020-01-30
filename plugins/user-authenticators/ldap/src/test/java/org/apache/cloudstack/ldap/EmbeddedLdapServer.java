/*-
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The
 * ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.registries.DefaultSchema;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.ldap.schema.loader.JarLdifSchemaLoader;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.JdbmPartitionFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Call init() to start the server and destroy() to shut it down.
 */
public class EmbeddedLdapServer {
    // API References:
    // http://directory.apache.org/apacheds/gen-docs/latest/apidocs/
    // http://directory.apache.org/api/gen-docs/latest/apidocs/

    private static final String BASE_PARTITION_NAME = "mydomain";
    private static final String BASE_DOMAIN = "org";
    private static final String BASE_STRUCTURE = "dc=" + BASE_PARTITION_NAME + ",dc=" + BASE_DOMAIN;

    private static final int LDAP_SERVER_PORT = 10389;
    private static final int BASE_CACHE_SIZE = 1000;
    private static final List<String> ATTR_NAMES_TO_INDEX = new ArrayList<String>(Arrays.asList("uid"));

    private DirectoryService _directoryService;
    private LdapServer _ldapServer;
    private JdbmPartition _basePartition;
    private boolean _deleteInstanceDirectoryOnStartup = true;
    private boolean _deleteInstanceDirectoryOnShutdown = true;

    public String getBasePartitionName() {
        return BASE_PARTITION_NAME;
    }

    public String getBaseStructure() {
        return BASE_STRUCTURE;
    }

    public int getBaseCacheSize() {
        return BASE_CACHE_SIZE;
    }

    public int getLdapServerPort() {
        return LDAP_SERVER_PORT;
    }

    public List<String> getAttrNamesToIndex() {
        return ATTR_NAMES_TO_INDEX;
    }

    protected void addSchemaExtensions() throws LdapException, IOException {
        // override to add custom attributes to the schema
    }

    public void init() throws Exception {
        if (getDirectoryService() == null) {
            if (getDeleteInstanceDirectoryOnStartup()) {
                deleteDirectory(getGuessedInstanceDirectory());
            }

            DefaultDirectoryServiceFactory serviceFactory = new DefaultDirectoryServiceFactory();
            serviceFactory.init(getDirectoryServiceName());
            setDirectoryService(serviceFactory.getDirectoryService());

            getDirectoryService().getChangeLog().setEnabled(false);
            getDirectoryService().setDenormalizeOpAttrsEnabled(true);

            createBasePartition();

            getDirectoryService().startup();

            createRootEntry();
        }

        if (getLdapServer() == null) {
            setLdapServer(new LdapServer());
            getLdapServer().setDirectoryService(getDirectoryService());
            getLdapServer().setTransports(new TcpTransport(getLdapServerPort()));
            getLdapServer().start();
        }
    }

    public void destroy() throws Exception {
        File instanceDirectory = getDirectoryService().getInstanceLayout().getInstanceDirectory();
        getLdapServer().stop();
        getDirectoryService().shutdown();
        setLdapServer(null);
        setDirectoryService(null);
        if (getDeleteInstanceDirectoryOnShutdown()) {
            deleteDirectory(instanceDirectory);
        }
    }

    public String getDirectoryServiceName() {
        return getBasePartitionName() + "DirectoryService";
    }

    private static void deleteDirectory(File path) throws IOException {
        FileUtils.deleteDirectory(path);
    }

    protected void createBasePartition() throws Exception {
        JdbmPartitionFactory jdbmPartitionFactory = new JdbmPartitionFactory();
        setBasePartition(jdbmPartitionFactory.createPartition(getDirectoryService().getSchemaManager(), getDirectoryService().getDnFactory(), getBasePartitionName(), getBaseStructure(), getBaseCacheSize(), getBasePartitionPath()));
        addSchemaExtensions();
        createBaseIndices();
        getDirectoryService().addPartition(getBasePartition());
    }

    protected void createBaseIndices() throws Exception {
        //
        // Default indices, that can be seen with getSystemIndexMap() and
        // getUserIndexMap(), are minimal.  There are no user indices by
        // default and the default system indices are:
        //
        // apacheOneAlias, entryCSN, apacheSubAlias, apacheAlias,
        // objectClass, apachePresence, apacheRdn, administrativeRole
        //
        for (String attrName : getAttrNamesToIndex()) {
            getBasePartition().addIndex(createIndexObjectForAttr(attrName));
        }
    }

    protected JdbmIndex<?> createIndexObjectForAttr(String attrName, boolean withReverse) throws LdapException {
        String oid = getOidByAttributeName(attrName);
        if (oid == null) {
            throw new RuntimeException("OID could not be found for attr " + attrName);
        }
        return new JdbmIndex(oid, withReverse);
    }

    protected JdbmIndex<?> createIndexObjectForAttr(String attrName) throws LdapException {
        return createIndexObjectForAttr(attrName, false);
    }

    protected void createRootEntry() throws LdapException {
        Entry entry = getDirectoryService().newEntry(getDirectoryService().getDnFactory().create(getBaseStructure()));
        entry.add("objectClass", "top", "domain", "extensibleObject");
        entry.add("dc", getBasePartitionName());
        CoreSession session = getDirectoryService().getAdminSession();
        try {
            session.add(entry);
        } finally {
            session.unbind();
        }
    }

    /**
     * @return A map where the key is the attribute name the value is the
     * oid.
     */
    public Map<String, String> getSystemIndexMap() throws IndexNotFoundException {
        Map<String, String> result = new LinkedHashMap<>();
        Iterator<String> it = getBasePartition().getSystemIndices();
        while (it.hasNext()) {
            String oid = it.next();
            Index<?, String> index = getBasePartition().getSystemIndex(getDirectoryService().getSchemaManager().getAttributeType(oid));
            result.put(index.getAttribute().getName(), index.getAttributeId());
        }
        return result;
    }

    /**
     * @return A map where the key is the attribute name the value is the
     * oid.
     */
    public Map<String, String> getUserIndexMap() throws IndexNotFoundException {
        Map<String, String> result = new LinkedHashMap<>();
        Iterator<String> it = getBasePartition().getUserIndices();
        while (it.hasNext()) {
            String oid = it.next();
            Index<?, String> index = getBasePartition().getUserIndex(getDirectoryService().getSchemaManager().getAttributeType(oid));
            result.put(index.getAttribute().getName(), index.getAttributeId());
        }
        return result;
    }

    public File getPartitionsDirectory() {
        return getDirectoryService().getInstanceLayout().getPartitionsDirectory();
    }

    public File getBasePartitionPath() {
        return new File(getPartitionsDirectory(), getBasePartitionName());
    }

    /**
     * Used at init time to clear out the likely instance directory before
     * anything is created.
     */
    public File getGuessedInstanceDirectory() {
        // See source code for DefaultDirectoryServiceFactory
        // buildInstanceDirectory.  ApacheDS looks at the workingDirectory
        // system property first and then defers to the java.io.tmpdir
        // system property.
        final String property = System.getProperty("workingDirectory");
        return new File(property != null ? property : System.getProperty("java.io.tmpdir") + File.separator + "server-work-" + getDirectoryServiceName());
    }

    public String getOidByAttributeName(String attrName) throws LdapException {
        return getDirectoryService().getSchemaManager().getAttributeTypeRegistry().getOidByName(attrName);
    }

    /**
     * Add additional schemas to the directory server. This takes a path to
     * the schema directory and uses the LdifSchemaLoader.
     *
     * @param schemaLocation The path to the directory containing the
     *                       "ou=schema" directory for an additional schema
     * @param schemaName     The name of the schema
     * @return true if the schemas have been loaded and the registries is
     * consistent
     */
    public boolean addSchemaFromPath(File schemaLocation, String schemaName) throws LdapException, IOException {
        LdifSchemaLoader schemaLoader = new LdifSchemaLoader(schemaLocation);
        DefaultSchema schema = new DefaultSchema(schemaLoader, schemaName);
        return getDirectoryService().getSchemaManager().load(schema);
    }

    /**
     * Add additional schemas to the directory server. This uses
     * JarLdifSchemaLoader, which will search for the "ou=schema" directory
     * within "/schema" on the classpath. If packaging the schema as part of
     * a jar using Gradle or Maven, you'd probably want to put your
     * "ou=schema" directory in src/main/resources/schema.
     * <p/>
     * It's also required that a META-INF/apacheds-schema.index be present in
     * your classpath that lists each LDIF file in your schema directory.
     *
     * @param schemaName The name of the schema
     * @return true if the schemas have been loaded and the registries is
     * consistent
     */
    public boolean addSchemaFromClasspath(String schemaName) throws LdapException, IOException {
        // To debug if your apacheds-schema.index isn't found:
        // Enumeration<URL> indexes = getClass().getClassLoader().getResources("META-INF/apacheds-schema.index");
        JarLdifSchemaLoader schemaLoader = new JarLdifSchemaLoader();
        Schema schema = schemaLoader.getSchema(schemaName);
        return schema != null && getDirectoryService().getSchemaManager().load(schema);
    }

    public DirectoryService getDirectoryService() {
        return _directoryService;
    }

    public void setDirectoryService(DirectoryService directoryService) {
        this._directoryService = directoryService;
    }

    public LdapServer getLdapServer() {
        return _ldapServer;
    }

    public void setLdapServer(LdapServer ldapServer) {
        this._ldapServer = ldapServer;
    }

    public JdbmPartition getBasePartition() {
        return _basePartition;
    }

    public void setBasePartition(JdbmPartition basePartition) {
        this._basePartition = basePartition;
    }

    public boolean getDeleteInstanceDirectoryOnStartup() {
        return _deleteInstanceDirectoryOnStartup;
    }

    public void setDeleteInstanceDirectoryOnStartup(boolean deleteInstanceDirectoryOnStartup) {
        this._deleteInstanceDirectoryOnStartup = deleteInstanceDirectoryOnStartup;
    }

    public boolean getDeleteInstanceDirectoryOnShutdown() {
        return _deleteInstanceDirectoryOnShutdown;
    }

    public void setDeleteInstanceDirectoryOnShutdown(boolean deleteInstanceDirectoryOnShutdown) {
        this._deleteInstanceDirectoryOnShutdown = deleteInstanceDirectoryOnShutdown;
    }

    public static void main (String[] args) {
        EmbeddedLdapServer embeddedLdapServer = new EmbeddedLdapServer();
        try {
            embeddedLdapServer.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
