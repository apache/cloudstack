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
package org.apache.cloudstack.storage.image;

import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.storage.EndPoint;

public interface ImageService {
    TemplateEntity registerTemplate(long templateId, long imageStoreId);

    boolean deleteTemplate(long templateId);

    long registerIso(String isoUrl, long accountId);

    boolean deleteIso(long isoId);

    boolean grantTemplateAccess(TemplateInfo template, EndPoint endpointId);

    boolean revokeTemplateAccess(long templateId, long endpointId);

    String grantIsoAccess(long isoId, long endpointId);

    boolean revokeIsoAccess(long isoId, long endpointId);

    TemplateEntity getTemplateEntity(long templateId);
}
