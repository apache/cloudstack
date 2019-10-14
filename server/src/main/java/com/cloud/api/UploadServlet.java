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

package com.cloud.api;

import static java.lang.System.out;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;

import com.cloud.utils.script.Script;

@WebServlet(urlPatterns={"/client/upload"}, name="upload")
public class UploadServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        Script.runSimpleBashScript("mkdir /tmp/upload");

        String location = "/tmp/upload";
        long maxFileSize = 1024 * 1024 * 1024;
        long maxRequestSize = 1024 * 1024 * 1024;
        int fileSizeThreshold = 64 * 1024;

        MultipartConfigElement multipartConfig = new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold);
        request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);

        response.setContentType("application/json;charset=UTF-8");
        ServletOutputStream os = response.getOutputStream();
        ServletConfig sc = getServletConfig();
        String path = sc.getInitParameter("location");

        Part filePart = request.getPart("file");
        String fileName = filePart.getSubmittedFileName();

        InputStream is = filePart.getInputStream();
        Files.copy(is, Paths.get(path + request.getPathInfo()), StandardCopyOption.REPLACE_EXISTING);

        response.setStatus(200);
        os.print("{ filePath: '" + Paths.get(path + request.getPathInfo()) + "' }");
    }
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String location = "/tmp/upload";
        String fileName = location + request.getPathInfo();
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(fileName);
            response.setContentType("application/octet-stream");
            OutputStream out = response.getOutputStream();
            IOUtils.copy(fis, out);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(fis);
        }
    }
}
