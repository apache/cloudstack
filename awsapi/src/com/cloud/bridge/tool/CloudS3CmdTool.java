// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.tool;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.axis2.AxisFault;
import org.apache.log4j.xml.DOMConfigurator;

import com.amazon.s3.client.AmazonS3Stub;
import com.amazon.s3.client.AmazonS3Stub.CreateBucket;
import com.amazon.s3.client.AmazonS3Stub.CreateBucketResponse;
import com.amazon.s3.client.AmazonS3Stub.DeleteBucket;
import com.amazon.s3.client.AmazonS3Stub.DeleteBucketResponse;
import com.amazon.s3.client.AmazonS3Stub.DeleteObject;
import com.amazon.s3.client.AmazonS3Stub.DeleteObjectResponse;
import com.amazon.s3.client.AmazonS3Stub.ListBucket;
import com.amazon.s3.client.AmazonS3Stub.ListBucketResponse;
import com.amazon.s3.client.AmazonS3Stub.ListBucketResult;
import com.amazon.s3.client.AmazonS3Stub.ListEntry;
import com.amazon.s3.client.AmazonS3Stub.PrefixEntry;
import com.amazon.s3.client.AmazonS3Stub.PutObjectInline;
import com.amazon.s3.client.AmazonS3Stub.PutObjectInlineResponse;
import com.amazon.s3.client.AmazonS3Stub.Status;

public class CloudS3CmdTool {
    private String serviceUrl;

    private AmazonS3Stub serviceStub;

    private static void configLog4j() {
        URL configUrl = System.class.getResource("/conf/log4j-cloud-bridge.xml");
        if (configUrl == null)
            configUrl = ClassLoader.getSystemResource("log4j-cloud-bridge.xml");

        if (configUrl == null)
            configUrl = ClassLoader.getSystemResource("conf/log4j-cloud-bridge.xml");

        if (configUrl != null) {
            try {
                System.out.println("Configure log4j using " + configUrl.toURI().toString());
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }

            try {
                File file = new File(configUrl.toURI());

                System.out.println("Log4j configuration from : " + file.getAbsolutePath());
                DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
            } catch (URISyntaxException e) {
                System.out.println("Unable to convert log4j configuration Url to URI");
            }
        } else {
            System.out.println("Configure log4j with default properties");
        }
    }

    private static Map<String, String> getNamedParameters(String[] args) {
        Map<String, String> params = new HashMap<String, String>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                String[] tokens = args[i].substring(1).split("=");
                if (tokens.length == 2) {
                    params.put(tokens[0], tokens[1]);
                }
            }
        }

        return params;
    }

    private static boolean validateNamedParameters(Map<String, String> params, String... keys) {
        for (String key : keys) {
            if (params.get(key) == null || params.get(key).isEmpty())
                return false;
        }
        return true;
    }

    public static void main(String[] args) {
        configLog4j();
        (new CloudS3CmdTool()).run(args);
    }

    private void run(String[] args) {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            if (envName.equals("CLOUD_SERVICE_URL"))
                serviceUrl = env.get(envName);
        }

        if (serviceUrl == null) {
            System.out.println("Please set CLOUD_SERVICE_URL environment variable");
            System.exit(0);
        }

        if (args.length < 1) {
            System.out.println("Please specify a command to run");
            System.exit(0);
        }

        try {
            serviceStub = new AmazonS3Stub(serviceUrl);
        } catch (AxisFault e) {
            System.out.println("Unable to initialize service stub");
            e.printStackTrace();
            System.exit(0);
        }

        // command dispatch
        if (args[0].equals("bucket-create")) {
            createBucket(args);
        } else if (args[0].equals("bucket-delete")) {
            deleteBucket(args);
        } else if (args[0].equals("bucket-list")) {
            listBucket(args);
        } else if (args[0].equals("object-put-inline")) {
            putObjectInline(args);
        } else if (args[0].equals("object-delete")) {
            deleteObject(args);
        }
    }

    private void createBucket(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: bucket-create <bucket-name>");
            System.exit(0);
        }

        try {
            CreateBucket bucket = new CreateBucket();
            bucket.setBucket(args[1]);
            bucket.setAWSAccessKeyId("TODO1");
            bucket.setSignature("TODO2");
            bucket.setTimestamp(Calendar.getInstance());

            CreateBucketResponse response = serviceStub.createBucket(bucket);
            System.out.println("Bucket " + response.getCreateBucketReturn().getBucketName() + " has been created successfully");
        } catch (Exception e) {
            System.out.println("Failed to execute bucket-create due to " + e.getMessage());
        }
    }

    private void deleteBucket(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: bucket-delete <bucket-name>");
            System.exit(0);
        }

        try {
            DeleteBucket request = new DeleteBucket();
            request.setBucket(args[1]);
            request.setSignature("TODO");
            request.setTimestamp(Calendar.getInstance());

            DeleteBucketResponse response = serviceStub.deleteBucket(request);
            Status status = response.getDeleteBucketResponse();
            if (status.getCode() == 200) {
                System.out.println("Bucket " + args[1] + " has been deleted successfully");
            } else {
                System.out.println("Unable to delete bucket " + args[1] + " - " + status.getDescription());
            }
        } catch (Exception e) {
            System.out.println("Failed to execute bucket-delete due to " + e.getMessage());
        }
    }

    private void listBucket(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: bucket-list -prefix=<prefix> -delimiter=<delimiter> -marker=<Marker> -max=<max items to return> <bucket name>");
            System.exit(0);
        }

        try {
            ListBucket request = new ListBucket();
            Map<String, String> params = getNamedParameters(args);

            request.setBucket(args[args.length - 1]);
            request.setCredential("TODO");
            if (params.get("prefix") != null)
                request.setPrefix(params.get("prefix"));
            if (params.get("delimiter") != null)
                request.setDelimiter(params.get("delimiter"));
            if (params.get("marker") != null)
                request.setMarker(params.get("marker"));
            if (params.get("max") != null) {
                try {
                    int maxKeys = Integer.parseInt(params.get("max"));
                    request.setMaxKeys(maxKeys);
                } catch (Exception e) {
                    System.out.println("-max parameter should be a numeric value");
                }
            }
            request.setAWSAccessKeyId("TODO");
            request.setCredential("TODO");
            request.setSignature("TODO");
            request.setTimestamp(Calendar.getInstance());

            ListBucketResponse response = serviceStub.listBucket(request);
            ListBucketResult result = response.getListBucketResponse();
            System.out.println("\tContent of Bucket " + result.getName());
            System.out.println("\tListing with prefix: " + result.getPrefix() + ", delimiter: " + result.getDelimiter() + ", marker: " + result.getMarker() + ", max: " +
                result.getMaxKeys());

            ListEntry[] entries = result.getContents();
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    ListEntry entry = entries[i];
                    System.out.print("\t");
                    System.out.print(entry.getSize());
                    System.out.print("\t");
                    System.out.print(entry.getKey());
                    System.out.print("\t");
                    System.out.print(entry.getETag());
                    System.out.print("\n");
                }
            }

            PrefixEntry[] prefixEntries = result.getCommonPrefixes();
            if (prefixEntries != null) {
                System.out.print("\n\n");

                for (int i = 0; i < prefixEntries.length; i++) {
                    System.out.print("\t<Prefix>\t");
                    System.out.print(prefixEntries[i].getPrefix());
                    System.out.print("\n");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to execute bucket-list due to " + e.getMessage());
        }
    }

    private void putObjectInline(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: object-put-inline -bucket=<bucket name> -name=<object name> <path to the file>");
            System.exit(0);
        }

        Map<String, String> params = getNamedParameters(args);
        if (!validateNamedParameters(params, "bucket", "name")) {
            System.out.println("Usage: object-put-inline -bucket=<bucket name> -name=<object name> <path to the file>");
            System.exit(0);
        }

        File file = new File(args[args.length - 1]);
        if (!file.exists()) {
            System.out.println("Unable to find file " + args[args.length - 1]);
            System.exit(0);
        }

        try {
            PutObjectInline request = new PutObjectInline();
            request.setBucket(params.get("bucket"));
            request.setKey(params.get("name"));
            request.setContentLength(file.length());
            request.setAWSAccessKeyId("TODO");
            request.setCredential("TODO");
            request.setSignature("TODO");
            request.setTimestamp(Calendar.getInstance());
            request.setData(new DataHandler(file.toURL()));

            PutObjectInlineResponse response = serviceStub.putObjectInline(request);
            System.out.println("Object has been posted successfully. ETag: " + response.getPutObjectInlineResponse().getETag());
        } catch (Exception e) {
            System.out.println("Failed to execute object-put-inline due to " + e.getMessage());
        }
    }

    private void deleteObject(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: object-delete -bucket=<bucket name> -name=<object name>");
            System.exit(0);
        }

        Map<String, String> params = getNamedParameters(args);
        if (!validateNamedParameters(params, "bucket", "name")) {
            System.out.println("Usage: object-delete -bucket=<bucket name> -name=<object name>");
            System.exit(0);
        }

        try {
            DeleteObject request = new DeleteObject();
            request.setAWSAccessKeyId("TODO");
            request.setBucket(params.get("bucket"));
            request.setKey(params.get("name"));
            request.setSignature("TODO");
            request.setCredential("TODO");
            request.setTimestamp(Calendar.getInstance());

            DeleteObjectResponse response = serviceStub.deleteObject(request);
            if (response.getDeleteObjectResponse().getCode() == 200)
                System.out.println("Object " + params.get("name") + " has been deleted successfully");
            else
                System.out.println("Object " + params.get("name") + " can not be deleted. Error: " + response.getDeleteObjectResponse().getCode());
        } catch (Exception e) {
            System.out.println("Failed to execute object-delete due to " + e.getMessage());
        }
    }
}
