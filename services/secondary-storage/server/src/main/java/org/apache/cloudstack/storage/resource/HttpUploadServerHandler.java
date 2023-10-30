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
package org.apache.cloudstack.storage.resource;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cloudstack.storage.template.UploadEntity;
import org.apache.cloudstack.utils.imagestore.ImageStoreUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

public class HttpUploadServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger logger = Logger.getLogger(HttpUploadServerHandler.class.getName());

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(true);

    private final StringBuilder responseContent = new StringBuilder();

    private HttpRequest request;

    private HttpPostRequestDecoder decoder;

    private NfsSecondaryStorageResource storageResource;

    private String uuid;

    private boolean requestProcessed = false;

    enum UploadHeader {
        SIGNATURE("x-signature"),
        METADATA("x-metadata"),
        EXPIRES("x-expires"),
        HOST("x-forwarded-host"),
        CONTENT_LENGTH("content-length");

        private final String name;
        UploadHeader(String name) {
            this.name = name;
        }
        public String getName() {
            return this.name;
        }
        public static UploadHeader fromName(String name) {
            for (UploadHeader type : values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    private static long processTimeout;

    protected Map<UploadHeader, String> getUploadRequestUsefulHeaders(HttpHeaders headers) {
        Map<UploadHeader, String> headerMap = new HashMap<>();
        for (Entry<String, String> entry : headers) {
            UploadHeader headerType = UploadHeader.fromName(entry.getKey());
            if (headerType != null) {
                headerMap.put(headerType, entry.getValue());
            }
        }
        for (UploadHeader type : UploadHeader.values()) {
            logger.info(String.format("HEADER: %s=%s", type, headerMap.get(type)));
        }
        return headerMap;
    }

    public HttpUploadServerHandler(NfsSecondaryStorageResource storageResource) {
        this.storageResource = storageResource;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (decoder != null) {
            decoder.cleanFiles();
        }
        requestProcessed = false;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!requestProcessed) {
            String message = "file receive failed or connection closed prematurely.";
            logger.error(message);
            storageResource.updateStateMapWithError(uuid, message);
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("HTTP request: %s", msg));
            }
            HttpRequest request = this.request = (HttpRequest) msg;
            responseContent.setLength(0);

            if (request.getMethod().equals(HttpMethod.POST)) {

                URI uri = new URI(request.getUri());

                Map<UploadHeader, String> headers = getUploadRequestUsefulHeaders(request.headers());
                long contentLength = headers.get(UploadHeader.CONTENT_LENGTH) != null ? Long.parseLong(headers.get(UploadHeader.CONTENT_LENGTH)) : 0;
                QueryStringDecoder decoderQuery = new QueryStringDecoder(uri);
                Map<String, List<String>> uriAttributes = decoderQuery.parameters();
                uuid = uriAttributes.get("uuid").get(0);
                logger.info("URI: uuid=" + uuid);
                UploadEntity uploadEntity = null;
                try {
                    // Validate the request here
                    storageResource.validatePostUploadRequest(headers.get(UploadHeader.SIGNATURE), headers.get(UploadHeader.METADATA),
                            headers.get(UploadHeader.EXPIRES), headers.get(UploadHeader.HOST), contentLength, uuid);
                    //create an upload entity. This will fail if entity already exists.
                    uploadEntity = storageResource.createUploadEntity(uuid, headers.get(UploadHeader.METADATA), contentLength);
                } catch (InvalidParameterValueException ex) {
                    logger.error("post request validation failed", ex);
                    responseContent.append(ex.getMessage());
                    writeResponse(ctx.channel(), HttpResponseStatus.BAD_REQUEST);
                    requestProcessed = true;
                    return;
                }
                if (uploadEntity == null) {
                    logger.error("Unable to create upload entity. An exception occurred.");
                    responseContent.append("Internal Server Error");
                    writeResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    requestProcessed = true;
                    return;
                }
                //set the base directory to download the file
                DiskFileUpload.baseDirectory = uploadEntity.getInstallPathPrefix();
                this.processTimeout = uploadEntity.getProcessTimeout();
                logger.info("base directory: " + DiskFileUpload.baseDirectory);
                try {
                    //initialize the decoder
                    decoder = new HttpPostRequestDecoder(factory, request);
                } catch (DecoderException e) {
                    logger.error("exception while initialising the decoder", e);
                    responseContent.append(e.getMessage());
                    writeResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    requestProcessed = true;
                    return;
                }
            } else {
                logger.warn("received a get request");
                responseContent.append("only post requests are allowed");
                writeResponse(ctx.channel(), HttpResponseStatus.BAD_REQUEST);
                requestProcessed = true;
                return;
            }

        }
        // check if the decoder was constructed before
        if (decoder != null) {
            if (msg instanceof HttpContent) {
                // New chunk is received
                HttpContent chunk = (HttpContent) msg;
                try {
                    decoder.offer(chunk);
                } catch (ErrorDataDecoderException e) {
                    logger.error("data decoding exception", e);
                    responseContent.append(e.getMessage());
                    writeResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    requestProcessed = true;
                    return;
                }
                if (chunk instanceof LastHttpContent) {
                    writeResponse(ctx.channel(), readFileUploadData());
                    reset();
                }
            }
        }

    }

    private void reset() {
        request = null;
        // destroy the decoder to release all resources
        decoder.destroy();
        decoder = null;
    }

    private HttpResponseStatus readFileUploadData() throws IOException {
        while (decoder.hasNext()) {
            InterfaceHttpData data = decoder.next();
            if (data != null) {
                try {
                    logger.info("BODY FileUpload: " + data.getHttpDataType().name() + ": " + data);
                    if (data.getHttpDataType() == HttpDataType.FileUpload) {
                        FileUpload fileUpload = (FileUpload) data;
                        if (fileUpload.isCompleted()) {
                            requestProcessed = true;
                            String format = ImageStoreUtil.checkTemplateFormat(fileUpload.getFile().getAbsolutePath(), fileUpload.getFilename());
                            if(StringUtils.isNotBlank(format)) {
                                String errorString = "File type mismatch between the sent file and the actual content. Received: " + format;
                                logger.error(errorString);
                                responseContent.append(errorString);
                                storageResource.updateStateMapWithError(uuid, errorString);
                                return HttpResponseStatus.BAD_REQUEST;
                            }
                            String status = storageResource.postUpload(uuid, fileUpload.getFile().getName(), processTimeout);
                            if (status != null) {
                                responseContent.append(status);
                                storageResource.updateStateMapWithError(uuid, status);
                                return HttpResponseStatus.INTERNAL_SERVER_ERROR;
                            } else {
                                responseContent.append("upload successful.");
                                return HttpResponseStatus.OK;
                            }
                        }
                    }
                } finally {
                    data.release();
                }
            }
        }
        responseContent.append("received entity is not a file");
        return HttpResponseStatus.UNPROCESSABLE_ENTITY;
    }

    private void writeResponse(Channel channel, HttpResponseStatus statusCode) {
        // Convert the response content to a ChannelBuffer.
        ByteBuf buf = copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
        responseContent.setLength(0);
        // Decide whether to close the connection or not.
        boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.headers().get(CONNECTION)) ||
            request.getProtocolVersion().equals(HttpVersion.HTTP_1_0) && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.headers().get(CONNECTION));
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, statusCode, buf);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        if (!close) {
            // There's no need to add 'content-length' header if this is the last response.
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
        }
        // Write the response.
        ChannelFuture future = channel.writeAndFlush(response);
        // Close the connection after the write operation is done if necessary.
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(responseContent.toString(), cause);
        responseContent.append("\r\nException occurred: ").append(cause.getMessage());
        writeResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
        ctx.channel().close();
    }
}
