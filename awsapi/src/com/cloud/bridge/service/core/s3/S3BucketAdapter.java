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
package com.cloud.bridge.service.core.s3;

import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;

import com.cloud.bridge.util.OrderedPair;

public interface S3BucketAdapter {
	void createContainer(String mountedRoot, String bucket);
	void deleteContainer(String mountedRoot, String bucket);
	String getBucketFolderDir(String mountedRoot, String bucket);
	String saveObject(InputStream is, String mountedRoot, String bucket, String fileName);
	DataHandler loadObject(String mountedRoot, String bucket, String fileName);
	DataHandler loadObjectRange(String mountedRoot, String bucket, String fileName, long startPos, long endPos);
	void deleteObject(String mountedRoot, String bucket, String fileName);
	OrderedPair<String, Long> concatentateObjects(String mountedRoot, String destBucket, String fileName, String sourceBucket, S3MultipartPart[] parts, OutputStream os);
}
