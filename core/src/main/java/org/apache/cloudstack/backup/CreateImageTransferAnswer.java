//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Answer;

public class CreateImageTransferAnswer extends Answer {
    private String imageTransferId;
    private String transferUrl;

    public CreateImageTransferAnswer() {
    }

    public CreateImageTransferAnswer(CreateImageTransferCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public CreateImageTransferAnswer(CreateImageTransferCommand cmd, boolean success, String details,
                                    String imageTransferId, String transferUrl) {
        super(cmd, success, details);
        this.imageTransferId = imageTransferId;
        this.transferUrl = transferUrl;
    }

    public String getImageTransferId() {
        return imageTransferId;
    }

    public void setImageTransferId(String imageTransferId) {
        this.imageTransferId = imageTransferId;
    }

    public String getTransferUrl() {
        return transferUrl;
    }

    public void setTransferUrl(String transferUrl) {
        this.transferUrl = transferUrl;
    }

}
