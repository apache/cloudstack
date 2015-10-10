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
package org.apache.cloudstack.quota.dao;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@Local(value = { QuotaBalanceDao.class })
public class QuotaBalanceDaoImpl extends GenericDaoBase<QuotaBalanceVO, Long> implements QuotaBalanceDao {
    private static final Logger s_logger = Logger.getLogger(QuotaBalanceDaoImpl.class.getName());

    @SuppressWarnings("deprecation")
    @Override
    public QuotaBalanceVO findLastBalanceEntry(final Long accountId, final Long domainId, final Date beforeThis) {
        List<QuotaBalanceVO> quotaBalanceEntries = new ArrayList<>();
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 1L);
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("creditsId", SearchCriteria.Op.EQ, 0);
            sc.addAnd("updatedOn", SearchCriteria.Op.LT, beforeThis);
            quotaBalanceEntries = this.search(sc, filter);
        } catch (Exception e) {
            s_logger.error("QuotaBalanceDaoImpl::findLastBalanceEntry() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to find last quota balance entry for account");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return quotaBalanceEntries.size() > 0 ? quotaBalanceEntries.get(0) : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public QuotaBalanceVO findLaterBalanceEntry(final Long accountId, final Long domainId, final Date afterThis) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaBalanceVO> quotaBalanceEntries = new ArrayList<>();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, 1L);
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("creditsId", SearchCriteria.Op.EQ, 0);
            sc.addAnd("updatedOn", SearchCriteria.Op.GT, afterThis);
            quotaBalanceEntries = this.search(sc, filter);
        } catch (Exception e) {
            s_logger.error("QuotaBalanceDaoImpl::findLaterBalanceEntry() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to find later quota balance entry");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return quotaBalanceEntries.size() > 0 ? quotaBalanceEntries.get(0) : null;
    }

    @Override
    public void saveQuotaBalance(final List<QuotaBalanceVO> credits) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            for (QuotaBalanceVO credit : credits) {
                persist(credit);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to save quota balance");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> findCreditBalance(final Long accountId, final Long domainId, final Date lastbalancedate, final Date beforeThis) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaBalanceVO> quotaBalances = new ArrayList<>();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("creditsId", SearchCriteria.Op.GT, 0);
            if ((lastbalancedate != null) && (beforeThis != null) && lastbalancedate.before(beforeThis)) {
                sc.addAnd("updatedOn", SearchCriteria.Op.BETWEEN, lastbalancedate, beforeThis);
            } else {
                return new ArrayList<QuotaBalanceVO>();
            }
            quotaBalances = search(sc, filter);
        } catch (Exception e) {
            s_logger.error("QuotaBalanceDaoImpl::findCreditBalance() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to find quota credit balance");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return quotaBalances;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> findQuotaBalance(final Long accountId, final Long domainId, final Date startDate, final Date endDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();

        List<QuotaBalanceVO> quotaUsageRecords = null;
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                sc.addAnd("updatedOn", SearchCriteria.Op.BETWEEN, startDate, endDate);
            } else {
                return Collections.<QuotaBalanceVO>emptyList();
            }
            quotaUsageRecords = listBy(sc);
            if (quotaUsageRecords.size() == 0) {
                quotaUsageRecords.addAll(lastQuotaBalanceVO(accountId, domainId, startDate));
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to find quota balance");
        } finally {
            TransactionLegacy.open(opendb).close();
        }

        return quotaUsageRecords;
    }


    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> lastQuotaBalanceVO(final Long accountId, final Long domainId, final Date pivotDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaBalanceVO> quotaUsageRecords = null;
        List<QuotaBalanceVO> trimmedRecords = new ArrayList<QuotaBalanceVO>();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 100L);
            // ASSUMPTION there will be less than 100 continuous credit
            // transactions
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            if ((pivotDate != null)) {
                sc.addAnd("updatedOn", SearchCriteria.Op.LTEQ, pivotDate);
            }
            quotaUsageRecords = search(sc, filter);

            // get records before startDate to find start balance
            for (QuotaBalanceVO entry : quotaUsageRecords) {
                s_logger.debug("FindQuotaBalance Date=" + entry.getUpdatedOn().toGMTString() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
                if (entry.getCreditsId() > 0) {
                    trimmedRecords.add(entry);
                } else {
                    trimmedRecords.add(entry);
                    break; // add only consecutive credit entries and last balance entry
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to get last quota balance");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return trimmedRecords;
    }

    @Override
    public BigDecimal lastQuotaBalance(final Long accountId, final Long domainId, Date startDate) {
        List<QuotaBalanceVO> quotaBalance = lastQuotaBalanceVO(accountId, domainId, startDate);
        if (quotaBalance.size() == 0) {
            new InvalidParameterValueException("There are no balance entries on or before the requested date.");
        }
        BigDecimal finalBalance = new BigDecimal(0);
        for (QuotaBalanceVO entry : quotaBalance) {
            s_logger.debug("lastQuotaBalance Date=" + entry.getUpdatedOn().toGMTString() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
            finalBalance = finalBalance.add(entry.getCreditBalance());
        }
        return finalBalance;
    }

}
