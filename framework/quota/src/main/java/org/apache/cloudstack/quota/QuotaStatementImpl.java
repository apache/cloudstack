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
import org.springframework.stereotype.Component;

import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ManagerBase;

@Component
public class QuotaStatementImpl extends ManagerBase implements QuotaStatement {

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

    public enum QuotaStatementPeriods {
        BIMONTHLY, MONTHLY, QUATERLY, HALFYEARLY, YEARLY
    };

    private QuotaStatementPeriods _period = QuotaStatementPeriods.MONTHLY;

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
        int period = period_str == null ? 1 : Integer.parseInt(period_str);

        QuotaStatementPeriods _period = QuotaStatementPeriods.values()[period];
        return true;
    }

    @Override
    public boolean start() {
        if (logger.isInfoEnabled()) {
            logger.info("Starting Statement Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (logger.isInfoEnabled()) {
            logger.info("Stopping Statement Manager");
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
                if (account != null) {
                    if (lastStatementDate == null || getDifferenceDays(lastStatementDate, new Date()) >= s_LAST_STATEMENT_SENT_DAYS + 1) {
                        BigDecimal quotaUsage = _quotaUsage.findTotalQuotaUsage(account.getAccountId(), account.getDomainId(), null, interval[0].getTime(), interval[1].getTime());
                        logger.info("For account=" + quotaAccount.getId() + ", quota used = " + quotaUsage);
                        // send statement
                        deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, quotaUsage, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_STATEMENT));
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("For " + quotaAccount.getId() + " the statement has been sent recently");

                        }
                    }
                }
            } else if (lastStatementDate != null) {
                logger.info("For " + quotaAccount.getId() + " it is already more than " + getDifferenceDays(lastStatementDate, new Date())
                        + " days, will send statement in next cycle");
            }
        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting to send quota STATEMENT email to users of account: " + emailToBeSent.getAccount().getAccountName());
            }
            _quotaAlert.sendQuotaAlert(emailToBeSent);
        }
    }

    @Override
    public Calendar[] getCurrentStatementTime() {
        final Calendar today = Calendar.getInstance();
        int day_of_month = today.get(Calendar.DAY_OF_MONTH);
        int month_of_year = today.get(Calendar.MONTH);

        Calendar firstDateOfCurrentPeriod, lastDateOfCurrentPeriod;
        Calendar aCalendar = (Calendar)today.clone();
        aCalendar.add(Calendar.DATE, 1);
        aCalendar.set(Calendar.HOUR, 0);
        aCalendar.set(Calendar.MINUTE, 0);
        aCalendar.set(Calendar.SECOND, 0);
        lastDateOfCurrentPeriod = aCalendar;

        switch (_period) {
        case BIMONTHLY:
            if (day_of_month < 16) {
                aCalendar = (Calendar)today.clone();
                aCalendar.add(Calendar.MONTH, 0);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            } else {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.DATE, 16);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            }
        case MONTHLY:
            aCalendar = (Calendar)today.clone();
            aCalendar.set(Calendar.DATE, 1);
            aCalendar.set(Calendar.HOUR, 0);
            aCalendar.set(Calendar.MINUTE, 0);
            aCalendar.set(Calendar.SECOND, 0);
            firstDateOfCurrentPeriod = aCalendar;
            return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
        case QUATERLY:
            if (month_of_year < Calendar.APRIL) {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.MONTH, Calendar.JANUARY);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            } else if (month_of_year < Calendar.JULY) {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.MONTH, Calendar.APRIL);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            } else if (month_of_year < Calendar.OCTOBER) {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.MONTH, Calendar.JULY);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            } else {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            }
        case HALFYEARLY:
            // statements are sent in Jan=1, Jul 7,
            if (month_of_year < Calendar.JULY) {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.MONTH, Calendar.JANUARY);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            } else {
                aCalendar = (Calendar)today.clone();
                aCalendar.set(Calendar.MONTH, Calendar.JULY);
                aCalendar.set(Calendar.DATE, 1);
                aCalendar.set(Calendar.HOUR, 0);
                aCalendar.set(Calendar.MINUTE, 0);
                aCalendar.set(Calendar.SECOND, 0);
                firstDateOfCurrentPeriod = aCalendar;
                return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
            }
        case YEARLY:
            aCalendar = (Calendar)today.clone();
            aCalendar.add(Calendar.MONTH, Calendar.JANUARY);
            aCalendar.set(Calendar.DATE, 1);
            aCalendar.set(Calendar.HOUR, 0);
            aCalendar.set(Calendar.MINUTE, 0);
            aCalendar.set(Calendar.SECOND, 0);
            firstDateOfCurrentPeriod = aCalendar;
            return new Calendar[] {firstDateOfCurrentPeriod, lastDateOfCurrentPeriod};
        default:
            break;
        }
        return null;
    }

    public Calendar[] statementTime(final Calendar today, final QuotaStatementPeriods period) {
        //check if it is statement time
        int day_of_month = today.get(Calendar.DAY_OF_MONTH);
        int month_of_year = today.get(Calendar.MONTH);
        Calendar firstDateOfPreviousPeriod, lastDateOfPreviousPeriod;
        switch (period) {
        case BIMONTHLY:
            if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                Calendar aCalendar = (Calendar)today.clone();
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
                Calendar aCalendar = (Calendar)today.clone();
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
                Calendar aCalendar = (Calendar)today.clone();
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
            if (month_of_year == Calendar.JANUARY || month_of_year == Calendar.APRIL || month_of_year == Calendar.JULY || month_of_year == Calendar.OCTOBER) {
                if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                    Calendar aCalendar = (Calendar)today.clone();
                    aCalendar.add(Calendar.MONTH, -3);
                    aCalendar.set(Calendar.DATE, 1);
                    aCalendar.set(Calendar.HOUR, 0);
                    aCalendar.set(Calendar.MINUTE, 0);
                    aCalendar.set(Calendar.SECOND, 0);
                    firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    aCalendar.add(Calendar.MONTH, 2);
                    aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                    lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
                }
            }
            return null;
        case HALFYEARLY:
            // statements are sent in Jan=1, Jul 7,
            if (month_of_year == Calendar.JANUARY || month_of_year == Calendar.JULY) {
                if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                    Calendar aCalendar = (Calendar)today.clone();
                    aCalendar.add(Calendar.MONTH, -6);
                    aCalendar.set(Calendar.DATE, 1);
                    aCalendar.set(Calendar.HOUR, 0);
                    aCalendar.set(Calendar.MINUTE, 0);
                    aCalendar.set(Calendar.SECOND, 0);
                    firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    aCalendar.add(Calendar.MONTH, 5);
                    aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
                    lastDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    return new Calendar[] {firstDateOfPreviousPeriod, lastDateOfPreviousPeriod};
                }
            }
            return null;
        case YEARLY:
            // statements are sent in Jan=1
            if (month_of_year == Calendar.JANUARY) {
                if (day_of_month < s_LAST_STATEMENT_SENT_DAYS) {
                    Calendar aCalendar = (Calendar)today.clone();
                    aCalendar.add(Calendar.MONTH, -12);
                    aCalendar.set(Calendar.DATE, 1);
                    aCalendar.set(Calendar.HOUR, 0);
                    aCalendar.set(Calendar.MINUTE, 0);
                    aCalendar.set(Calendar.SECOND, 0);
                    firstDateOfPreviousPeriod = (Calendar)aCalendar.clone();
                    aCalendar.add(Calendar.MONTH, 11);
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
