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
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.secstorage.HeuristicVO;
import org.apache.cloudstack.secstorage.dao.SecondaryStorageHeuristicDao;
import org.apache.cloudstack.secstorage.heuristics.HeuristicPurpose;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.heuristics.presetvariables.PresetVariables;
import org.apache.cloudstack.storage.heuristics.presetvariables.SecondaryStorage;
import org.apache.cloudstack.storage.heuristics.presetvariables.Snapshot;
import org.apache.cloudstack.storage.heuristics.presetvariables.Template;
import org.apache.cloudstack.storage.heuristics.presetvariables.Volume;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for building and injecting the heuristics preset variables into the JS script.
 */
public class HeuristicRuleHelper {

    protected static final Logger LOGGER = Logger.getLogger(HeuristicRuleHelper.class);

    private static final Long HEURISTICS_SCRIPT_TIMEOUT = StorageManager.HEURISTICS_SCRIPT_TIMEOUT.value();

    @Inject
    private DataStoreManager dataStoreManager;

    @Inject
    private ImageStoreDao imageStoreDao;

    @Inject
    private SecondaryStorageHeuristicDao secondaryStorageHeuristicDao;

    /**
     * Returns the {@link DataStore} object if the zone, specified by the ID, has an active heuristic rule for the given {@link HeuristicPurpose}.
     * It returns null otherwise.
     * @param zoneId used to search for the heuristic rules.
     * @param heuristicPurpose used for checking if there is a heuristic rule for the given {@link HeuristicPurpose}.
     * @param obj can be from the following classes: {@link VMTemplateVO}, {@link SnapshotInfo} and {@link VolumeVO}.
     *           They are used to retrieve attributes for injecting in the JS rule.
     * @return the corresponding {@link DataStore} if there is a heuristic rule, returns null otherwise.
     */
    public DataStore getImageStoreIfThereIsHeuristicRule(Long zoneId, HeuristicPurpose heuristicPurpose, Object obj) {
        HeuristicVO heuristicsVO = secondaryStorageHeuristicDao.findByZoneIdAndPurpose(zoneId, heuristicPurpose);

        if (heuristicsVO == null) {
            LOGGER.debug(String.format("No heuristic rules found for zone with ID [%s] and heuristic purpose [%s]. Returning null.", zoneId, heuristicPurpose));
            return null;
        } else {
            LOGGER.debug(String.format("Found the heuristic rule %s to apply for zone with ID [%s].", heuristicsVO, zoneId));
            return interpretHeuristicRule(heuristicsVO.getHeuristicRule(), heuristicPurpose, obj, zoneId);
        }
    }

    /**
     * Build the preset variables ({@link Template}, {@link Snapshot} and {@link Volume}) for the JS script.
     */
    protected void buildPresetVariables(JsInterpreter jsInterpreter, HeuristicPurpose heuristicPurpose, long zoneId, Object obj) {
        PresetVariables presetVariables = new PresetVariables();

        switch (heuristicPurpose) {
            case TEMPLATE:
            case ISO:
                presetVariables.setTemplate(setTemplatePresetVariable((VMTemplateVO) obj));
                break;
            case SNAPSHOT:
                presetVariables.setSnapshot(setSnapshotPresetVariable((SnapshotInfo) obj));
                break;
            case VOLUME:
                presetVariables.setVolume(setVolumePresetVariable((VolumeVO) obj));
                break;
        }
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
    }

    protected List<SecondaryStorage> setSecondaryStoragesVariable(long zoneId) {
        List<SecondaryStorage> secondaryStorageList = new ArrayList<>();
        List<ImageStoreVO> imageStoreVOS = imageStoreDao.listStoresByZoneId(zoneId);

        for (ImageStoreVO imageStore : imageStoreVOS) {
            SecondaryStorage secondaryStorage = new SecondaryStorage();

            secondaryStorage.setUuid(imageStore.getUuid());
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

        template.setFormat(templateVO.getFormat());
        template.setHypervisorType(templateVO.getHypervisorType());

        return template;
    }

    protected Volume setVolumePresetVariable(VolumeVO volumeVO) {
        Volume volume = new Volume();

        volume.setFormat(volumeVO.getFormat());
        volume.setSize(volumeVO.getSize());

        return volume;
    }

    protected Snapshot setSnapshotPresetVariable(SnapshotInfo snapshotInfo) {
        Snapshot snapshot = new Snapshot();

        snapshot.setSize(snapshotInfo.getSize());
        snapshot.setHypervisorType(snapshotInfo.getHypervisorType());

        return snapshot;
    }

    /**
     * This method calls {@link HeuristicRuleHelper#buildPresetVariables(JsInterpreter, HeuristicPurpose, long, Object)} for building the preset variables and
     * execute the JS script specified in the <b>rule</b> ({@link String}) parameter. The script is pre-injected with the preset variables, to allow the JS script to reference them
     * in the code scope.
     * <br>
     * <br>
     * The JS script needs to return a valid UUID ({@link String}) of a secondary storage, otherwise a {@link CloudRuntimeException} is thrown.
     * @param rule the {@link String} representing the JS script.
     * @param heuristicPurpose used for building the preset variables accordingly to the  {@link HeuristicPurpose} specified.
     * @param obj can be from the following classes: {@link VMTemplateVO}, {@link SnapshotInfo} and {@link VolumeVO}.
     *           They are used to retrieve attributes for injecting in the JS rule.
     * @param zoneId used for injecting the {@link SecondaryStorage} preset variables.
     * @return the {@link DataStore} returned by the script.
     */
    public DataStore interpretHeuristicRule(String rule, HeuristicPurpose heuristicPurpose, Object obj, long zoneId) {
        try (JsInterpreter jsInterpreter = new JsInterpreter(HEURISTICS_SCRIPT_TIMEOUT)) {
            buildPresetVariables(jsInterpreter, heuristicPurpose, zoneId, obj);
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
            LOGGER.error(message, ex);
            throw new CloudRuntimeException(message, ex);
        }
    }

}
