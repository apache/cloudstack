//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.QuotaAlertManagerImpl.DeferredQuotaEmail;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.log4j.Logger;

import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ManagerBase;

public class QuotaStatementImpl extends ManagerBase implements QuotaStatement {
    private static final Logger s_logger = Logger.getLogger(QuotaAlertManagerImpl.class);

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private QuotaUsageDao _quotaUsage;
    @Inject
    private QuotaAlertManager _quotaAlert;
    @Inject
    private ConfigurationDao _configDao;

    final public static int s_LAST_STATEMENT_SENT_DAYS = 6; //ideally should be less than 7 days

    public enum STATEMENT_PERIODS {
        BIMONTHLY, MONTHLY, QUATERLY, HALFYEARLY, YEARLY
    };

    private STATEMENT_PERIODS _period = STATEMENT_PERIODS.MONTHLY;

    public QuotaStatementImpl() {
        super();
    }

    private void mergeConfigs(Map<String, String> dbParams, Map<String, Object> xmlParams) {
        for (Map.Entry<String, Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        Map<String, String> configs = _configDao.getConfiguration(params);

        if (params != null) {
            mergeConfigs(configs, params);
        }
        String period_str = configs.get(QuotaConfig.QuotaStatementPeriod.key());
        int period = period_str == null ? 1 : Integer.valueOf(period_str);

        STATEMENT_PERIODS _period = STATEMENT_PERIODS.values()[period];
        return true;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Starting Alert Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Alert Manager");
        }
        return true;
    }

    @Override
    public void sendStatement() {

        List<DeferredQuotaEmail> deferredQuotaEmailList = new ArrayList<DeferredQuotaEmail>();
        for (final QuotaAccountVO quotaAccount : _quotaAcc.listAllQuotaAccount()) {
            if (quotaAccount.getQuotaBalance() == null) {
                continue; // no quota usage for this account ever, ignore
            }

            //check if it is statement time
            Calendar interval[] = statementTime(Calendar.getInstance(), _period);

            Date lastStatementDate = quotaAccount.getLastStatementDate();
            if (interval != null) {
                AccountVO account = _accountDao.findById(quotaAccount.getId());
                if (lastStatementDate == null || getDifferenceDays(lastStatementDate, new Date()) >= s_LAST_STATEMENT_SENT_DAYS + 1) {
                    BigDecimal quotaUsage = _quotaUsage.findTotalQuotaUsage(account.getAccountId(), account.getDomainId(), null, interval[0].getTime(), interval[1].getTime());
                    s_logger.info("For account=" + quotaAccount.getId() + ", quota used = " + quotaUsage);
                    // send statement
                    deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, quotaUsage, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_STATEMENT));
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("For " + quotaAccount.getId() + " the statement has been sent recently");

                    }
                }
            } else if (lastStatementDate != null) {
                s_logger.info("For " + quotaAccount.getId() + " it is already more than " + getDifferenceDays(lastStatementDate, new Date())
                        + " days, will send statement in next cycle");
            }
        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Attempting to send quota STATEMENT email to users of account: " + emailToBeSent.getAccount().getAccountName());
            }
            _quotaAlert.sendQuotaAlert(emailToBeSent);
        }
    }

    public Calendar[] statementTime(final Calendar today, final STATEMENT_PERIODS period) {
        //check if it is statement time
        int day_of_month = today.get(Calendar.DAY_OF_MONTH);
        int month_of_year = today.get(Calendar.MONTH);
        Calendar firstDateOfPreviousPeriod, lastDateOfPreviousPeriod;
        switch (period) {
        case BIMONTHLY:
            if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                Calendar aCalendar = Calendar.getInstance();
                aCalendar.add(Calendar.MONTH, 0);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                aCalendar.set(Calendar.DATE, 15);
                lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
            } else if (day_of_month > 15 && (day_of_month - 15) < s_LAST_STATEMENT_SENT_DAYS) {
                Calendar aCalendar = Calendar.getInstance();
                aCalendar.add(Calendar.MONTH, -1);
                aCalendar.set(Calendar.DATE, 16);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
            }
            return null;
        case MONTHLY:
            if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                Calendar aCalendar = Calendar.getInstance();
                aCalendar.add(Calendar.MONTH, -1);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
            }
            return null;
        case QUATERLY:
            // statements are sent in Jan=1, Apr 4, Jul 7, Oct 10
            if (month_of_year == 1 || month_of_year == 4 || month_of_year == 7 || month_of_year == 10) {
                if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                    Calendar aCalendar = Calendar.getInstance();
                    aCalendar.add(Calendar.MONTH, -3);
                    aCalendar.set(Calendar.DATE, 1);
                    aCalendar.set(Calendar.HOUR, 0);
                    aCalendar.set(Calendar.MINUTE, 0);
                    aCalendar.set(Calendar.SECOND, 0);
                    firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    aCalendar.add(Calendar.MONTH, -1);
                    aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                    lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
                }
            }
            return null;
        case HALFYEARLY:
            // statements are sent in Jan=1, Jul 7,
            if (month_of_year == 1 || month_of_year == 7) {
                if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                    Calendar aCalendar = Calendar.getInstance();
                    aCalendar.add(Calendar.MONTH, -6);
                    aCalendar.set(Calendar.DATE, 1);
                    aCalendar.set(Calendar.HOUR, 0);
                    aCalendar.set(Calendar.MINUTE, 0);
                    aCalendar.set(Calendar.SECOND, 0);
                    firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    aCalendar.add(Calendar.MONTH, -1);
                    aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                    lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
                }
            }
            return null;
        case YEARLY:
            // statements are sent in Jan=1
            if (month_of_year == 1) {
                if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                    Calendar aCalendar = Calendar.getInstance();
                    aCalendar.add(Calendar.MONTH, -12);
                    aCalendar.set(Calendar.DATE, 1);
                    aCalendar.set(Calendar.HOUR, 0);
                    aCalendar.set(Calendar.MINUTE, 0);
                    aCalendar.set(Calendar.SECOND, 0);
                    firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    aCalendar.add(Calendar.MONTH, -1);
                    aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                    lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
                }
            }
            return null;
        default:
            break;
        }
        return null;
    }

    public static long getDifferenceDays(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

}
