/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.admin.annotation;

import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

public class AddAnnotationCmdTest {

    private AddAnnotationCmd addAnnotationCmd = new AddAnnotationCmd();

    @Mock
    public AnnotationService annotationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        addAnnotationCmd.annotationService = annotationService;
    }

    @Test
    public void properEntityType() throws Exception {
        addAnnotationCmd.setEntityType("HOST");
        addAnnotationCmd.setEntityUuid("1");
        AnnotationResponse annotationResponse = spy(new AnnotationResponse());
        when(annotationService.addAnnotation(addAnnotationCmd)).thenReturn(annotationResponse);
        addAnnotationCmd.execute();
        verify(annotationResponse).setResponseName("addannotationresponse");
    }

    @Test (expected = IllegalStateException.class)
    public void wrongEntityType() throws Exception {
        addAnnotationCmd.setEntityType("BLA");
        addAnnotationCmd.setEntityUuid("1");
        addAnnotationCmd.execute();
    }
}
