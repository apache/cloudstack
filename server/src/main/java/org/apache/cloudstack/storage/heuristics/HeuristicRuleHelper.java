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
package org.apache.cloudstack.storage.heuristics;

import com.cloud.api.ApiDBUtils;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.secstorage.HeuristicVO;
import org.apache.cloudstack.secstorage.dao.SecondaryStorageHeuristicDao;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.heuristics.presetvariables.Account;
import org.apache.cloudstack.storage.heuristics.presetvariables.Domain;
import org.apache.cloudstack.storage.heuristics.presetvariables.PresetVariables;
import org.apache.cloudstack.storage.heuristics.presetvariables.SecondaryStorage;
import org.apache.cloudstack.storage.heuristics.presetvariables.Snapshot;
import org.apache.cloudstack.storage.heuristics.presetvariables.Template;
import org.apache.cloudstack.storage.heuristics.presetvariables.Volume;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for building and injecting the heuristics preset variables into the JS script.
 */
public class HeuristicRuleHelper {

    protected Logger logger = LogManager.getLogger(HeuristicRuleHelper.class);

    private static final Long HEURISTICS_SCRIPT_TIMEOUT = StorageManager.HEURISTICS_SCRIPT_TIMEOUT.value();

    @Inject
    private DataStoreManager dataStoreManager;

    @Inject
    private ImageStoreDao imageStoreDao;

    @Inject
    private SecondaryStorageHeuristicDao secondaryStorageHeuristicDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private AccountDao accountDao;

    /**
     * Returns the {@link DataStore} object if the zone, specified by the ID, has an active heuristic rule for the given {@link HeuristicType}.
     * It returns null otherwise.
     * @param zoneId used to search for the heuristic rules.
     * @param heuristicType used for checking if there is a heuristic rule for the given {@link HeuristicType}.
     * @param obj can be from the following classes: {@link VMTemplateVO}, {@link SnapshotInfo} and {@link VolumeVO}.
     *           They are used to retrieve attributes for injecting in the JS rule.
     * @return the corresponding {@link DataStore} if there is a heuristic rule, returns null otherwise.
     */
    public DataStore getImageStoreIfThereIsHeuristicRule(Long zoneId, HeuristicType heuristicType, Object obj) {
        HeuristicVO heuristicsVO = secondaryStorageHeuristicDao.findByZoneIdAndType(zoneId, heuristicType);

        if (heuristicsVO == null) {
            logger.debug(String.format("No heuristic rules found for zone with ID [%s] and heuristic type [%s]. Returning null.", zoneId, heuristicType));
            return null;
        } else {
            logger.debug(String.format("Found the heuristic rule %s to apply for zone with ID [%s].", heuristicsVO, zoneId));
            return interpretHeuristicRule(heuristicsVO.getHeuristicRule(), heuristicType, obj, zoneId);
        }
    }

    /**
     * Build the preset variables ({@link Template}, {@link Snapshot} and {@link Volume}) for the JS script.
     */
    protected void buildPresetVariables(JsInterpreter jsInterpreter, HeuristicType heuristicType, long zoneId, Object obj) {
        PresetVariables presetVariables = new PresetVariables();
        Long accountId = null;

        switch (heuristicType) {
            case TEMPLATE:
            case ISO:
                presetVariables.setTemplate(setTemplatePresetVariable((VMTemplateVO) obj));
                accountId = ((VMTemplateVO) obj).getAccountId();
                break;
            case SNAPSHOT:
                presetVariables.setSnapshot(setSnapshotPresetVariable((SnapshotInfo) obj));
                accountId = ((SnapshotInfo) obj).getAccountId();
                break;
            case VOLUME:
                presetVariables.setVolume(setVolumePresetVariable((VolumeVO) obj));
                accountId = ((VolumeVO) obj).getAccountId();
                break;
        }
        presetVariables.setAccount(setAccountPresetVariable(accountId));
        presetVariables.setSecondaryStorages(setSecondaryStoragesVariable(zoneId));

        injectPresetVariables(jsInterpreter, presetVariables);
    }

    /**
     * Inject the {@link PresetVariables} ({@link Template}, {@link Snapshot} and {@link Volume}) into the JS {@link JsInterpreter}.
     * For each type, they can be accessed using the following variables in the script:
     * <ul>
     *     <li>ISO/Template: using the variable <b>iso</b> or <b>template</b>, e.g. <b>iso.format</b> </li>
     *     <li>Snapshot: using the variable <b>snapshot</b>, e.g. <b>snapshot.size</b></li>
     *     <li>Volume: using the variable <b>volume</b>, e.g. <b>volume.format</b></li>
     * </ul>
     * @param jsInterpreter the JS script
     * @param presetVariables used for injecting in the JS interpreter.
     */
    protected void injectPresetVariables(JsInterpreter jsInterpreter, PresetVariables presetVariables) {
        jsInterpreter.injectVariable("secondaryStorages", presetVariables.getSecondaryStorages().toString());

        if (presetVariables.getTemplate() != null) {
            jsInterpreter.injectVariable("template", presetVariables.getTemplate().toString());
            jsInterpreter.injectVariable("iso", presetVariables.getTemplate().toString());
        }

        if (presetVariables.getSnapshot() != null) {
            jsInterpreter.injectVariable("snapshot", presetVariables.getSnapshot().toString());
        }

        if (presetVariables.getVolume() != null) {
            jsInterpreter.injectVariable("volume", presetVariables.getVolume().toString());
        }

        if (presetVariables.getAccount() != null) {
            jsInterpreter.injectVariable("account", presetVariables.getAccount().toString());
        }
    }

    protected List<SecondaryStorage> setSecondaryStoragesVariable(long zoneId) {
        List<SecondaryStorage> secondaryStorageList = new ArrayList<>();
        List<ImageStoreVO> imageStoreVOS = imageStoreDao.listStoresByZoneId(zoneId);

        for (ImageStoreVO imageStore : imageStoreVOS) {
            SecondaryStorage secondaryStorage = new SecondaryStorage();

            secondaryStorage.setName(imageStore.getName());
            secondaryStorage.setId(imageStore.getUuid());
            secondaryStorage.setProtocol(imageStore.getProtocol());
            StorageStats storageStats = ApiDBUtils.getSecondaryStorageStatistics(imageStore.getId());

            if (storageStats != null) {
                secondaryStorage.setUsedDiskSize(storageStats.getByteUsed());
                secondaryStorage.setTotalDiskSize(storageStats.getCapacityBytes());
            }

            secondaryStorageList.add(secondaryStorage);
        }
        return secondaryStorageList;
    }

    protected Template setTemplatePresetVariable(VMTemplateVO templateVO) {
        Template template = new Template();

        template.setName(templateVO.getName());
        template.setFormat(templateVO.getFormat());
        template.setHypervisorType(templateVO.getHypervisorType());

        return template;
    }

    protected Volume setVolumePresetVariable(VolumeVO volumeVO) {
        Volume volume = new Volume();

        volume.setName(volumeVO.getName());
        volume.setFormat(volumeVO.getFormat());
        volume.setSize(volumeVO.getSize());

        return volume;
    }

    protected Snapshot setSnapshotPresetVariable(SnapshotInfo snapshotInfo) {
        Snapshot snapshot = new Snapshot();

        snapshot.setName(snapshotInfo.getName());
        snapshot.setSize(snapshotInfo.getSize());
        snapshot.setHypervisorType(snapshotInfo.getHypervisorType());

        return snapshot;
    }

    protected Account setAccountPresetVariable(Long accountId) {
        if (accountId == null) {
            return null;
        }

        AccountVO account = accountDao.findById(accountId);
        if (account == null) {
            return null;
        }

        Account accountVariable = new Account();
        accountVariable.setName(account.getName());
        accountVariable.setId(account.getUuid());

        accountVariable.setDomain(setDomainPresetVariable(account.getDomainId()));

        return accountVariable;
    }

    protected Domain setDomainPresetVariable(long domainId) {
        DomainVO domain = domainDao.findById(domainId);
        if (domain == null) {
            return null;
        }
        Domain domainVariable = new Domain();
        domainVariable.setName(domain.getName());
        domainVariable.setId(domain.getUuid());

        return domainVariable;
    }

    /**
     * This method calls {@link HeuristicRuleHelper#buildPresetVariables(JsInterpreter, HeuristicType, long, Object)} for building the preset variables and
     * execute the JS script specified in the <b>rule</b> ({@link String}) parameter. The script is pre-injected with the preset variables, to allow the JS script to reference them
     * in the code scope.
     * <br>
     * <br>
     * The JS script needs to return a valid UUID ({@link String}) of a secondary storage, otherwise a {@link CloudRuntimeException} is thrown.
     * @param rule the {@link String} representing the JS script.
     * @param heuristicType used for building the preset variables accordingly to the  {@link HeuristicType} specified.
     * @param obj can be from the following classes: {@link VMTemplateVO}, {@link SnapshotInfo} and {@link VolumeVO}.
     *           They are used to retrieve attributes for injecting in the JS rule.
     * @param zoneId used for injecting the {@link SecondaryStorage} preset variables.
     * @return the {@link DataStore} returned by the script.
     */
    public DataStore interpretHeuristicRule(String rule, HeuristicType heuristicType, Object obj, long zoneId) {
        try (JsInterpreter jsInterpreter = new JsInterpreter(HEURISTICS_SCRIPT_TIMEOUT)) {
            buildPresetVariables(jsInterpreter, heuristicType, zoneId, obj);
            Object scriptReturn = jsInterpreter.executeScript(rule);

            if (!(scriptReturn instanceof String)) {
                throw new CloudRuntimeException(String.format("Error while interpreting heuristic rule [%s], the rule did not return a String.", rule));
            }

            DataStore dataStore = dataStoreManager.getImageStoreByUuid((String) scriptReturn);

            if (dataStore == null) {
                throw new CloudRuntimeException(String.format("Unable to find a secondary storage with the UUID [%s] returned by the heuristic rule [%s]. Check if the rule is " +
                        "returning a valid UUID.", scriptReturn, rule));
            }

            return dataStore;
        } catch (IOException ex) {
            String message = String.format("Error while executing script [%s].", rule);
            logger.error(message, ex);
            throw new CloudRuntimeException(message, ex);
        }
    }

}
