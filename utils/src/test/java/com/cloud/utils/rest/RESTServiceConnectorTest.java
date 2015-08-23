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

package com.cloud.utils.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.CollectionType;
import org.junit.Test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class RESTServiceConnectorTest {
    private static final BasicStatusLine HTTP_200_STATUS_LINE = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private static final Map<String, String> DEFAULT_TEST_PARAMETERS = new HashMap<String, String>();
    static {
        DEFAULT_TEST_PARAMETERS.put("arg1", "val1");
        DEFAULT_TEST_PARAMETERS.put("arg2", "val2");
    }

    @Test
    public void testExecuteUpdateObject() throws Exception {
        final TestPojo newObject = new TestPojo();
        newObject.setField("newValue");
        final String newObjectJson = gson.toJson(newObject);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        connector.executeUpdateObject(newObject, "/somepath");

        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("PUT"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPayloadMatcher.aPayload(newObjectJson), any(HttpClientContext.class));
    }

    @Test
    public void testExecuteUpdateObjectWithParameters() throws Exception {
        final TestPojo newObject = new TestPojo();
        newObject.setField("newValue");
        final String newObjectJson = gson.toJson(newObject);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");

        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        connector.executeUpdateObject(newObject, "/somepath", DEFAULT_TEST_PARAMETERS);

        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("PUT"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPayloadMatcher.aPayload(newObjectJson), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("arg2=val2"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("arg1=val1"), any(HttpClientContext.class));
    }

    @Test
    public void testExecuteCreateObject() throws Exception {
        final TestPojo newObject = new TestPojo();
        newObject.setField("newValue");
        final String newObjectJson = gson.toJson(newObject);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(new StringEntity(newObjectJson));
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        final TestPojo object = connector.executeCreateObject(newObject, "/somepath");

        assertThat(object, notNullValue());
        assertThat(object, equalTo(newObject));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("POST"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPayloadMatcher.aPayload(newObjectJson), any(HttpClientContext.class));
        verify(response).close();
    }

    @Test
    public void testExecuteCreateObjectWithParameters() throws Exception {
        final TestPojo newObject = new TestPojo();
        newObject.setField("newValue");
        final String newObjectJson = gson.toJson(newObject);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(new StringEntity(newObjectJson));
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        final TestPojo object = connector.executeCreateObject(newObject, "/somepath", DEFAULT_TEST_PARAMETERS);

        assertThat(object, notNullValue());
        assertThat(object, equalTo(newObject));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("POST"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestPayloadMatcher.aPayload(newObjectJson), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("arg2=val2"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("arg1=val1"), any(HttpClientContext.class));
        verify(response).close();
    }

    @Test
    public void testExecuteDeleteObject() throws Exception {
        final HttpEntity entity = mock(HttpEntity.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(entity);
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        connector.executeDeleteObject("/somepath");

        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("DELETE"), any(HttpClientContext.class));
        verify(response).close();
    }

    @Test
    public void testExecuteRetrieveObject() throws Exception {
        final TestPojo existingObject = new TestPojo();
        existingObject.setField("existingValue");
        final String newObjectJson = gson.toJson(existingObject);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(new StringEntity(newObjectJson));
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        final TestPojo object = connector.executeRetrieveObject(TestPojo.class, "/somepath");

        assertThat(object, notNullValue());
        assertThat(object, equalTo(existingObject));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("GET"), any(HttpClientContext.class));
        verify(response).close();
    }

    @Test
    public void testExecuteRetrieveObjectWithParameters() throws Exception {
        final TestPojo existingObject = new TestPojo();
        existingObject.setField("existingValue");
        final String newObjectJson = gson.toJson(existingObject);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(new StringEntity(newObjectJson));
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder().client(restClient).build();

        final TestPojo object = connector.executeRetrieveObject(TestPojo.class, "/somepath", DEFAULT_TEST_PARAMETERS);

        assertThat(object, notNullValue());
        assertThat(object, equalTo(existingObject));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestMethodMatcher.aMethod("GET"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("arg2=val2"), any(HttpClientContext.class));
        verify(httpClient).execute(any(HttpHost.class), HttpUriRequestQueryMatcher.aQueryThatContains("arg1=val1"), any(HttpClientContext.class));
        verify(response).close();
    }

    @Test(expected = JsonParseException.class)
    public void testCustomDeserializerTypeMismatch() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        when(response.getEntity()).thenReturn(new StringEntity("[{somethig_not_type : \"WrongType\"}]"));
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final RESTServiceConnector connector = new RESTServiceConnector.Builder()
            .client(restClient)
            .classToDeserializerEntry(TestPojo.class, new TestPojoDeserializer())
            .build();

        connector.executeRetrieveObject(TestPojo.class, "/somepath");
    }

    @Test
    public void testCustomDeserializerForCustomLists() throws Exception {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(HTTP_200_STATUS_LINE);
        when(response.getEntity()).thenReturn(new StringEntity("{results: [{field : \"SomeValue\"}], results_count: 1}"));
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpClientContext.class))).thenReturn(response);
        final RestClient restClient = new BasicRestClient(httpClient, HttpClientContext.create(), "localhost");
        final Class<? extends CollectionType> clazzListOfTestPojo = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, TestPojo.class).getClass();
        final RESTServiceConnector connector = new RESTServiceConnector.Builder()
            .client(restClient)
            .classToDeserializerEntry(clazzListOfTestPojo, new CustomListDeserializer<TestPojoDeserializer>())
            .build();

        connector.executeRetrieveObject(TestPojo.class, "/somepath");
    }

    class NiciraList<T> {
        private List<T> results;
        private int resultCount;

        public List<T> getResults() {
            return results;
        }

        public void setResults(final List<T> results) {
            this.results = results;
        }

        public int getResultCount() {
            return resultCount;
        }

        public void setResultCount(final int resultCount) {
            this.resultCount = resultCount;
        }

    }

    class TestPojo {
        private String field;

        public String getField() {
            return field;
        }

        public void setField(final String field) {
            this.field = field;
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(final Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

    }

    private final class TestPojoDeserializer implements JsonDeserializer<TestPojo> {
        @Override
        public TestPojo deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Deserializing as a TestPojo, but no type present in the json object");
            }

            return context.deserialize(jsonObject, TestPojo.class);
        }
    }

    private final class CustomListDeserializer<T> implements JsonDeserializer<T> {
        private final Gson standardGson = new GsonBuilder().create();

        @Override
        public T deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();

            System.err.println(json.toString());

            if (jsonObject.has("results")) {
                final JsonArray results = jsonObject.getAsJsonArray("results");
                return context.deserialize(results, typeOfT);
            } else {
                return standardGson.fromJson(jsonObject, typeOfT);
            }
        }
    }
}
