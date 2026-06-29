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
package org.apache.cloudstack.gui.theme;

import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.user.gui.theme.CreateGuiThemeCmd;
import org.apache.cloudstack.api.command.user.gui.theme.ListGuiThemesCmd;
import org.apache.cloudstack.api.command.user.gui.theme.RemoveGuiThemeCmd;
import org.apache.cloudstack.api.command.user.gui.theme.UpdateGuiThemeCmd;
import org.apache.cloudstack.api.response.GuiThemeResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.gui.theme.dao.GuiThemeDao;
import org.apache.cloudstack.gui.theme.dao.GuiThemeDetailsDao;
import org.apache.cloudstack.gui.theme.dao.GuiThemeJoinDao;
import org.apache.cloudstack.gui.theme.json.config.validator.JsonConfigValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class GuiThemeServiceImpl implements GuiThemeService {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    GuiThemeDao guiThemeDao;

    @Inject
    GuiThemeDetailsDao guiThemeDetailsDao;

    @Inject
    GuiThemeJoinDao guiThemeJoinDao;

    @Inject
    ResponseGenerator responseGenerator;

    @Inject
    EntityManager entityManager;

    @Inject
    AccountDao accountDao;

    @Inject
    DomainDao domainDao;

    @Inject
    JsonConfigValidator jsonConfigValidator;

    @Override
    public ListResponse<GuiThemeResponse> listGuiThemes(ListGuiThemesCmd cmd) {
        ListResponse<GuiThemeResponse> response = new ListResponse<>();
        Pair<List<GuiThemeJoinVO>, Integer> result;
        boolean listOnlyDefaultTheme = cmd.getListOnlyDefaultTheme();

        if (listOnlyDefaultTheme) {
            result = retrieveDefaultTheme();
        } else if (CallContext.current().getCallingAccountId() == Account.ACCOUNT_ID_SYSTEM) {
            logger.info("Unauthenticated call to `listGuiThemes` API, ignoring all parameters, except `commonName`.");
            result = listGuiThemesWithNoAuthentication(cmd);
        } else {
            result = listGuiThemesInternal(cmd);
        }
        List<GuiThemeResponse> guiThemeResponses = new ArrayList<>();

        for (GuiThemeJoin guiThemeJoin : result.first()) {
            GuiThemeResponse guiThemeResponse = responseGenerator.createGuiThemeResponse(guiThemeJoin);
            guiThemeResponses.add(guiThemeResponse);
        }

        response.setResponses(guiThemeResponses);
        return response;
    }

    private Pair<List<GuiThemeJoinVO>, Integer> retrieveDefaultTheme() {
        GuiThemeJoinVO defaultTheme = guiThemeJoinDao.findDefaultTheme();
        List<GuiThemeJoinVO> list = new ArrayList<>();

        if (defaultTheme != null) {
            list.add(defaultTheme);
        }

        return new Pair<>(list, list.size());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUI_THEME_CREATE, eventDescription = "Creating GUI theme")
    public GuiThemeJoin createGuiTheme(CreateGuiThemeCmd cmd) {
        String name = cmd.getName();
        String description = cmd.getDescription();
        String css = cmd.getCss();
        String jsonConfiguration = cmd.getJsonConfiguration();
        String commonNames = cmd.getCommonNames();
        String providedDomainIds = cmd.getDomainIds();
        String providedAccountIds = cmd.getAccountIds();
        boolean isPublic = cmd.getPublic();
        Boolean recursiveDomains = cmd.getRecursiveDomains();

        CallContext.current().setEventDetails(String.format("Name: %s, AccountIDs: %s, DomainIDs: %s, RecursiveDomains: %s, CommonNames: %s", name, providedAccountIds,
                providedDomainIds, recursiveDomains, commonNames));

        if (StringUtils.isAllBlank(css, jsonConfiguration)) {
            throw new CloudRuntimeException("Either the `css` or `jsonConfiguration` parameter must be informed.");
        }

        validateParameters(jsonConfiguration, providedDomainIds, providedAccountIds, commonNames, null);

        if (shouldSetGuiThemeToPrivate(providedDomainIds, providedAccountIds)) {
            isPublic = false;
        }

        GuiThemeVO guiThemeVO = new GuiThemeVO(name, description, css, jsonConfiguration, recursiveDomains, isPublic, new Date(), null);
        guiThemeDao.persist(guiThemeVO);
        persistGuiThemeDetails(guiThemeVO.getId(), commonNames, providedDomainIds, providedAccountIds);
        return guiThemeJoinDao.findById(guiThemeVO.getId());
    }

    protected void persistGuiThemeDetails(long guiThemeId, String commonNames, String providedDomainIds, String providedAccountIds) {
        persistDetailValueIfNotNull(guiThemeId, commonNames, "commonName");
        persistDetailValueIfNotNull(guiThemeId, providedDomainIds, "domain");
        persistDetailValueIfNotNull(guiThemeId, providedAccountIds, "account");
    }

    protected void persistDetailValueIfNotNull(long guiThemeId, String providedParameter, String type) {
        if (providedParameter == null) {
            logger.trace("GUI theme provided parameter `{}` is null; therefore, it will be ignored.", type);
            return;
        }
        for (String splitParameter : StringUtils.deleteWhitespace(providedParameter).split(",")) {
            guiThemeDetailsDao.persist(new GuiThemeDetailsVO(guiThemeId, type, splitParameter));
        }

    }

    protected boolean shouldSetGuiThemeToPrivate(String providedDomainIds, String providedAccountIds) {
        if (StringUtils.isNotBlank(providedAccountIds)) {
            logger.info("Parameter `accountIds` was informed during GUI theme creation, therefore, `isPublic` will be set to `false`.");
            return true;
        }

        if (StringUtils.isNotBlank(providedDomainIds)) {
            logger.info("Parameter `domainIds` was informed during GUI theme creation, therefore, `isPublic` will be set to `false`.");
            return true;
        }
        return false;
    }

    /**
     * A GUI theme is only considered the default one if the parameters `commonNames`, `domainIds` and `accountIds` are all blank.
     * @return true if all parameters are blank, false otherwise.
     */
    protected boolean isConsideredDefaultTheme(String commonNames, String providedDomainIds, String providedAccountIds) {
        return StringUtils.isAllBlank(commonNames, providedDomainIds, providedAccountIds);
    }

    /**
     * There can only be one default theme registered, therefore, a {@link CloudRuntimeException} will be thrown if:
     * <ul>
     *     <li>There is already a default theme registered when creating a new GUI theme.</li>
     *     <li>Or, the GUI theme to be updated is not the default theme already registered.</li>
     * </ul>
     */
    protected void checkIfDefaultThemeIsAllowed(String commonNames, String providedDomainIds, String providedAccountIds, Long idOfThemeToBeUpdated) {
        if (!isConsideredDefaultTheme(commonNames, providedDomainIds, providedAccountIds)) {
            logger.info("The GUI theme will not be considered as the default one, as the `commonNames`, `domainIds` and `accountIds` are not all blank.");
            return;
        }

        GuiThemeJoinVO defaultTheme = guiThemeJoinDao.findDefaultTheme();

        if (defaultTheme != null && (idOfThemeToBeUpdated == null || defaultTheme.getId() != idOfThemeToBeUpdated)) {
            throw new CloudRuntimeException(String.format("Only one default GUI theme is allowed. Remove the current default theme %s and try again.", defaultTheme));
        }

        logger.info("The parameters `commonNames`, `domainIds` and `accountIds` were not informed. The created theme will be considered as the default theme.");
    }

    protected Pair<List<GuiThemeJoinVO>, Integer> listGuiThemesWithNoAuthentication(ListGuiThemesCmd cmd) {
        return guiThemeJoinDao.listGuiThemesWithNoAuthentication(cmd.getCommonName());
    }


    protected Pair<List<GuiThemeJoinVO>, Integer> listGuiThemesInternal(ListGuiThemesCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        String commonName = cmd.getCommonName();
        String domainUuid = cmd.getDomainId() == null ? null : domainDao.findById(cmd.getDomainId()).getUuid();
        String accountUuid = cmd.getAccountId() == null ? null : accountDao.findById(cmd.getAccountId()).getUuid();
        boolean listAll = cmd.getListAll();
        boolean showRemoved = cmd.getShowRemoved();
        Boolean showPublic = cmd.getShowPublic();

        return guiThemeJoinDao.listGuiThemes(id, name, commonName, domainUuid, accountUuid, listAll, showRemoved, showPublic);
    }

    protected void validateParameters(String jsonConfig, String domainIds, String accountIds, String commonNames, Long idOfThemeToBeUpdated) {
        if (isConsideredDefaultTheme(commonNames, domainIds, accountIds)) {
            checkIfDefaultThemeIsAllowed(commonNames, domainIds, accountIds, idOfThemeToBeUpdated);
        }

        validateObjectUuids(accountIds, Account.class);
        validateObjectUuids(domainIds, Domain.class);
        jsonConfigValidator.validateJsonConfiguration(jsonConfig);
    }

    /**
     * Validate if the comma separated list of UUIDs of the fields {@link GuiThemeJoinVO#getAccounts()} and {@link GuiThemeJoinVO#getDomains()} are valid.
     * @param providedIds a comma separated list of UUIDs of {@link Account} or {@link Domain}
     * @param clazz the class to infer the DAO object. Valid options are: {@link Account} and {@link Domain}
     */
    protected void validateObjectUuids(String providedIds, Class clazz) {
        if (StringUtils.isBlank(providedIds)) {
            return;
        }

        String[] commaSeparatedIds = providedIds.split("\\s*,\\s*");
        for (String id : commaSeparatedIds) {
            Object objectVO = entityManager.findByUuid(clazz, id);

            if (objectVO == null) {
                throw new CloudRuntimeException(String.format("The %s ID %s does not exist. Verify the informed IDs and try again.", clazz.getSimpleName(), id));
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUI_THEME_UPDATE, eventDescription = "Updating GUI theme")
    public GuiThemeJoin updateGuiTheme(UpdateGuiThemeCmd cmd) {
        Long guiThemeId = cmd.getId();
        GuiThemeJoinVO guiThemeJoinVO = guiThemeJoinDao.findById(guiThemeId);

        String name = cmd.getName();
        String description = cmd.getDescription();
        String css = cmd.getCss();
        String jsonConfiguration = cmd.getJsonConfiguration();
        String commonNames = cmd.getCommonNames() == null ? guiThemeJoinVO.getCommonNames() : cmd.getCommonNames();
        String providedDomainIds = cmd.getDomainIds() == null ? guiThemeJoinVO.getDomains() : cmd.getDomainIds();
        String providedAccountIds = cmd.getAccountIds() == null ? guiThemeJoinVO.getAccounts() : cmd.getAccountIds();
        Boolean isPublic = cmd.getIsPublic();
        Boolean recursiveDomains = cmd.getRecursiveDomains();

        CallContext.current().setEventDetails(String.format("ID: %s, Name: %s, AccountIDs: %s, DomainIDs: %s, RecursiveDomains: %s, CommonNames: %s", guiThemeId, name,
                providedAccountIds, providedDomainIds, recursiveDomains, commonNames));

        validateParameters(jsonConfiguration, providedDomainIds, providedAccountIds, commonNames, guiThemeId);

        if (shouldSetGuiThemeToPrivate(providedDomainIds, providedAccountIds)) {
            isPublic = false;
        }

        return persistGuiTheme(guiThemeId, name, description, css, jsonConfiguration, commonNames, providedDomainIds, providedAccountIds, isPublic, recursiveDomains);
    }

    protected GuiThemeJoinVO persistGuiTheme(Long guiThemeId, String name, String description, String css, String jsonConfiguration, String commonNames, String providedDomainIds,
                                             String providedAccountIds, Boolean isPublic, Boolean recursiveDomains){
        return Transaction.execute((TransactionCallback<GuiThemeJoinVO>) status -> {
            GuiThemeVO guiThemeVO = guiThemeDao.findById(guiThemeId);

            if (name != null) {
                guiThemeVO.setName(ifBlankReturnNull(name));
            }

            if (description != null) {
                guiThemeVO.setDescription(ifBlankReturnNull(description));
            }

            if (css != null) {
                guiThemeVO.setCss(css);
            }

            if (jsonConfiguration != null) {
                guiThemeVO.setJsonConfiguration(jsonConfiguration);
            }

            if (isPublic != null) {
                guiThemeVO.setIsPublic(isPublic);
            }

            if (recursiveDomains != null) {
                guiThemeVO.setRecursiveDomains(recursiveDomains);
            }

            logger.trace("Persisting GUI theme [{}] with CSS [{}] and JSON configuration [{}].", guiThemeVO, guiThemeVO.getCss(), guiThemeVO.getJsonConfiguration());

            guiThemeDao.persist(guiThemeVO);
            guiThemeDetailsDao.expungeByGuiThemeId(guiThemeId);
            persistGuiThemeDetails(guiThemeId, commonNames, providedDomainIds, providedAccountIds);

            return guiThemeJoinDao.findById(guiThemeId);
        });
    }

    protected String ifBlankReturnNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUI_THEME_REMOVE, eventDescription = "Removing GUI theme")
    public void removeGuiTheme(RemoveGuiThemeCmd cmd) {
        Long guiThemeId = cmd.getId();
        GuiThemeVO guiThemeVO = guiThemeDao.findById(guiThemeId);

        if (guiThemeVO != null) {
            CallContext.current().setEventDetails(String.format("ID: %s", guiThemeVO.getUuid()));
            guiThemeDao.remove(guiThemeId);
        } else {
            logger.error("Unable to find a GUI theme with the specified ID [{}].", guiThemeId);
            throw new CloudRuntimeException("Unable to find a GUI theme with the specified ID.");
        }
    }
}
