package org.apache.cloudstack.storage.datastore.util;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.cloud.storage.Storage;

@RunWith(JUnit4.class)
public class NexentaUtilTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testParseNmsUrl() {
        NexentaNmsUrl c;

        c = NexentaUtil.parseNmsUrl("auto://192.168.1.1/");
        assertEquals(c.toString(), "auto://admin:nexenta@192.168.1.1:2000/rest/nms/");
        assertEquals(c.getSchema(), "http");

        c = NexentaUtil.parseNmsUrl("http://192.168.1.1/");
        assertEquals(c.toString(), "http://admin:nexenta@192.168.1.1:2000/rest/nms/");

        c = NexentaUtil.parseNmsUrl("http://192.168.1.1:8080");
        assertEquals(c.toString(), "http://admin:nexenta@192.168.1.1:8080/rest/nms/");

        c = NexentaUtil.parseNmsUrl("https://root@192.168.1.1:8080");
        assertEquals(c.toString(), "https://root:nexenta@192.168.1.1:8080/rest/nms/");

        c = NexentaUtil.parseNmsUrl("https://root:password@192.168.1.1:8080");
        assertEquals(c.toString(), "https://root:password@192.168.1.1:8080/rest/nms/");
    }

    @Test
    public void testGetStorageType() {
        assertEquals(NexentaUtil.getStorageType("iscsi"), Storage.StoragePoolType.Iscsi);
        assertEquals(NexentaUtil.getStorageType("nfs"), Storage.StoragePoolType.NetworkFilesystem);
        assertEquals(NexentaUtil.getStorageType("any"), Storage.StoragePoolType.Iscsi);
    }

    @Test
    public void testParseNexentaPluginUrl() {
        String url = "nmsUrl=http://admin:nexenta@192.168.1.1:2000;";

        NexentaUtil.NexentaPluginParameters parameters;
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getNmsUrl().toString(), "http://admin:nexenta@192.168.1.1:2000/rest/nms/");
        assertNull(parameters.getVolume());
        assertEquals(parameters.getStorageType(), Storage.StoragePoolType.Iscsi);
        assertEquals(parameters.getStorageHost(), "192.168.1.1");
        assertEquals((int) parameters.getStoragePort(), NexentaUtil.DEFAULT_ISCSI_TARGET_PORTAL_PORT);
        assertNull(parameters.getStoragePath());
        assertEquals((boolean) parameters.getSparseVolumes(), false);
        assertEquals(parameters.getVolumeBlockSize(), "8K");

        url += "volume=cloudstack";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getNmsUrl().toString(), "http://admin:nexenta@192.168.1.1:2000/rest/nms/");
        assertEquals(parameters.getVolume(), "cloudstack");

        url += "/;";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getVolume(), "cloudstack");

        url += "storageType=";
        parameters = NexentaUtil.parseNexentaPluginUrl(url + "nfs");
        assertEquals(parameters.getStorageType(), Storage.StoragePoolType.NetworkFilesystem);
        assertEquals((int) parameters.getStoragePort(), NexentaUtil.DEFAULT_NFS_PORT);

        parameters = NexentaUtil.parseNexentaPluginUrl(url + "iscsi");
        assertEquals(parameters.getStorageType(), Storage.StoragePoolType.Iscsi);
        assertEquals((int) parameters.getStoragePort(), NexentaUtil.DEFAULT_ISCSI_TARGET_PORTAL_PORT);

        url += "nfs;storageHost=192.168.1.2;";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getStorageHost(), "192.168.1.2");

        url += "storagePort=3000;";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals((int) parameters.getStoragePort(), 3000);

        url += "storagePath=/volumes/cloudstack;";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getStoragePath(), "/volumes/cloudstack");

        url += "sparseVolumes=true;";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getSparseVolumes(), Boolean.TRUE);

        url += "volumeBlockSize=128K;";
        parameters = NexentaUtil.parseNexentaPluginUrl(url);
        assertEquals(parameters.getVolumeBlockSize(), "128K");

        url += "unknownParameter=value;";  // NOTE: exception should not be raised
        parameters = NexentaUtil.parseNexentaPluginUrl(url);

        assertEquals(parameters.getNmsUrl().toString(), "http://admin:nexenta@192.168.1.1:2000/rest/nms/");
        assertEquals(parameters.getVolume(), "cloudstack");
        assertEquals(parameters.getStorageType(), Storage.StoragePoolType.NetworkFilesystem);
        assertEquals(parameters.getStorageHost(), "192.168.1.2");
        assertEquals((int) parameters.getStoragePort(), 3000);
        assertEquals(parameters.getStoragePath(), "/volumes/cloudstack");
        assertEquals(parameters.getSparseVolumes(), Boolean.TRUE);
        assertEquals(parameters.getVolumeBlockSize(), "128K");

        exception.expect(RuntimeException.class);
        exception.expectMessage("Invalid URL format");

        NexentaUtil.parseNexentaPluginUrl(url + "invalidParameter");
    }
}
