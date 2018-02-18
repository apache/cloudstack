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
package com.cloud.test.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Formatter;

public class SqlDataGenerator {
    public static void main(String[] args) {
        try {
            FileOutputStream fs = new FileOutputStream(new File("out.txt"));

            DataOutputStream out = new DataOutputStream(fs);

            for (int i = 20; i < 171; i++) {
                out.writeBytes("INSERT INTO `vmops`.`dc_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES ('192.168.2." + i + "',1,1);\r\n");
            }
            out.writeBytes("\r\n");
            for (int i = 1; i < 10000; i++) {
                StringBuilder imagePath = new StringBuilder();
                Formatter formatter = new Formatter(imagePath);
                formatter.format("%04x", i);
                out.writeBytes("INSERT INTO `vmops`.`dc_vnet_alloc` (vnet, data_center_id) VALUES ('" + imagePath.toString() + "',1);\r\n");
            }

            out.flush();
            out.close();
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "error during sql generation: " + e.getLocalizedMessage());
        }
    }
}
