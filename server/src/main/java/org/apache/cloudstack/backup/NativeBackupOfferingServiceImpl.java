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
package org.apache.cloudstack.backup;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.api.command.user.backup.nativeoffering.CreateNativeBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.nativeoffering.DeleteNativeBackupOfferingCmd;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.NativeBackupOfferingDao;

import javax.inject.Inject;

public class NativeBackupOfferingServiceImpl extends ManagerBase implements NativeBackupOfferingService {
    @Inject
    private NativeBackupOfferingDao nativeBackupOfferingDao;

    @Inject
    private BackupOfferingDao backupOfferingDao;

    @Override
    public NativeBackupOffering createNativeBackupOffering(CreateNativeBackupOfferingCmd cmd) {
        NativeBackupOfferingVO offeringVO = new NativeBackupOfferingVO(cmd.getName(), cmd.isCompress(), cmd.isValidate(), cmd.getValidationSteps(), cmd.isAllowQuickRestore(),
                cmd.isAllowExtractFile(), cmd.getBackupChainSize(), cmd.getCompressionLibrary());
        return nativeBackupOfferingDao.persist(offeringVO);
    }

    @Override
    public NativeBackupOffering deleteNativeBackupOffering(DeleteNativeBackupOfferingCmd cmd) {
        NativeBackupOfferingVO nativeOfferingVO = nativeBackupOfferingDao.findByIdIncludingRemoved(cmd.getId());

        if (nativeOfferingVO.getRemoved() != null) {
            logger.info("Offering [%s] is already deleted.");
            return nativeOfferingVO;
        }

        BackupOffering offeringVO = backupOfferingDao.findByExternalId(nativeOfferingVO.getUuid(), null);

        if (offeringVO != null) {
            throw new InvalidParameterValueException(String.format("Cannot remove a native backup offering that is in use. Currently imported offering is [%s].",
                    offeringVO.getName()));
        }

        nativeBackupOfferingDao.remove(cmd.getId());

        return nativeBackupOfferingDao.findByIdIncludingRemoved(cmd.getId());
    }
}
