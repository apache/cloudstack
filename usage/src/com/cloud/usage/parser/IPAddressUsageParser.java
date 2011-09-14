/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.parser;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.usage.UsageIPAddressVO;
import com.cloud.usage.UsageServer;
import com.cloud.usage.UsageTypes;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageIPAddressDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.ComponentLocator;

public class IPAddressUsageParser {
	public static final Logger s_logger = Logger.getLogger(IPAddressUsageParser.class.getName());
	
	private static ComponentLocator _locator = ComponentLocator.getLocator(UsageServer.Name, "usage-components.xml", "log4j-cloud_usage");
	private static UsageDao m_usageDao = _locator.getDao(UsageDao.class);
	private static UsageIPAddressDao m_usageIPAddressDao = _locator.getDao(UsageIPAddressDao.class);
	
	// FIXME:  IP Address stuff will be in the helper table and not really rolled up to usage table since it doesn't make sense to have it that way
	public static boolean parse(AccountVO account, Date startDate, Date endDate) {
		s_logger.info("Parsing all ip address usage events");

		// FIXME: endDate should be 23:59:59 of the day in question if it's not after the current date (or null)
		if ((endDate == null) || endDate.after(new Date())) {
		    endDate = new Date();
		}

        List<UsageIPAddressVO> usageInstances = m_usageIPAddressDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate, false, null, null);

        // IP Addresses are billed monthly.  In the given date range, figure out how many months occur and create a usage record
        // for each month
        // FIXME:  as part of this usage record, we might want to say startTime/endTime during the month that the IP was allocated
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        // set the end date to be the last day of the month
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        int numberOfMonths = 0;
        while (startCal.before(endCal)) {
            numberOfMonths++;
            startCal.roll(Calendar.MONTH, true);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("processing " + numberOfMonths + " month(s) worth of ip address data");
        }

        for (UsageIPAddressVO usageInstance : usageInstances) {
            String ipAddress = usageInstance.getAddress();
            Date assignedDate = usageInstance.getAssigned();
            Date releasedDate = usageInstance.getReleased();

            // if the IP address is currently owned, bill for up to the current date
            if (releasedDate == null) {
                releasedDate = new Date();
            }

            // reset startCal
            startCal.setTime(startDate);
            startCal.set(Calendar.DAY_OF_MONTH, 1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            // TODO: this really needs to be tested well, and might be over-engineered for what we really need, but the
            //       point is to count each month in which the IP address is owned and bill for that month
            // we know the number of months, create a usage record for each month
            // FIXME:  this is supposed to create a usage record per month...first of all, that's super confusing and we need
            //         to get out of the weekly/monthly/daily business and instead we need to say for a given range whether or
            //         not the IP address was use.  It's up to our customers to (a) give sensible date ranges for their own
            //         usage purposes and (b) 
            for (int i = 0; i < numberOfMonths; i++) {
                if (assignedDate.before(startCal.getTime())) {
                    assignedDate = startCal.getTime();
                }
                startCal.roll(Calendar.MONTH, true);
                Date nextMonth = startCal.getTime();
                startCal.add(Calendar.MILLISECOND, -1);
                if (releasedDate.before(startCal.getTime())) {
                    startCal.setTime(releasedDate);
                }
                createUsageRecord(assignedDate, startCal.getTime(), account, ipAddress, startDate, endDate);

                // go to the start of the next month for the next iteration
                startCal.setTime(nextMonth);
            }
        }

		return true;
	}

	// TODO: ip address usage comes from the usage_ip_address table, not cloud_usage table, so this is largely irrelevant and might be going away
	private static void createUsageRecord(Date assigned, Date ownedUntil, AccountVO account, String address, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating usage record for account: " + account.getId() + ", ip: " + address + ", assigned date: " + assigned + ", owned until: " + ownedUntil);
        }

        // Create the usage record
        String usageDesc = "usage for ip address '" + address +
                             "' (assigned on " + assigned + ", owned until " + ownedUntil + ")";
        UsageVO usageRecord = new UsageVO(Long.valueOf(0), account.getId(), account.getDomainId(), usageDesc, "1 Month", UsageTypes.IP_ADDRESS, Double.valueOf(1),
                                                null, null, null, null, null, null, startDate, endDate);
        m_usageDao.persist(usageRecord);
    }
}
