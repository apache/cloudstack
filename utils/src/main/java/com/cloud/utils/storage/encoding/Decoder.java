//
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
//

package com.cloud.utils.storage.encoding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class Decoder {
    private static final Logger s_logger = Logger.getLogger(Decoder.class);

    private static Map<String, String> getParameters(URI uri) {
        String parameters = uri.getQuery();
        Map<String, String> params = new HashMap<String, String>();
        List<String> paraLists = Arrays.asList(parameters.split("&"));
        for (String para : paraLists) {
            String[] pair = para.split("=");
            if (!pair[1].equalsIgnoreCase("null")) {
                params.put(pair[0], pair[1]);
            }

        }
        return params;
    }

    public static DecodedDataObject decode(String url) throws URISyntaxException {
        URI uri = new URI(url);
        Map<String, String> params = getParameters(uri);
        DecodedDataStore store =
            new DecodedDataStore(params.get(EncodingType.ROLE.toString()), params.get(EncodingType.STOREUUID.toString()),
                params.get(EncodingType.PROVIDERNAME.toString()), uri.getScheme(), uri.getScheme() + uri.getHost() + uri.getPath(), uri.getHost(), uri.getPath());

        Long size = null;
        try {
            size = Long.parseLong(params.get(EncodingType.SIZE.toString()));
        } catch (NumberFormatException e) {
            s_logger.info("[ignored] number not recognised",e);
        }
        DecodedDataObject obj =
            new DecodedDataObject(params.get(EncodingType.OBJTYPE.toString()), size, params.get(EncodingType.NAME.toString()), params.get(EncodingType.PATH.toString()),
                store);
        return obj;
    }
}
