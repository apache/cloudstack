/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.chainsaw;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.table.AbstractTableModel;
import org.apache.log4j.Priority;
import org.apache.log4j.Logger;

/**
 * Represents a list of <code>EventDetails</code> objects that are sorted on
 * logging time. Methods are provided to filter the events that are visible.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
class MyTableModel
    extends AbstractTableModel
{

    /** used to log messages **/
    private static final Logger LOG = Logger.getLogger(MyTableModel.class);

    /** use the compare logging events **/
    private static final Comparator MY_COMP = new Comparator()
    {
        /** @see Comparator **/
        public int compare(Object aObj1, Object aObj2) {
            if ((aObj1 == null) && (aObj2 == null)) {
                return 0; // treat as equal
            } else if (aObj1 == null) {
                return -1; // null less than everything
            } else if (aObj2 == null) {
                return 1; // think about it. :->
            }

            // will assume only have LoggingEvent
            final EventDetails le1 = (EventDetails) aObj1;
            final EventDetails le2 = (EventDetails) aObj2;

            if (le1.getTimeStamp() < le2.getTimeStamp()) {
                return 1;
            }
            // assume not two events are logged at exactly the same time
            return -1;
        }
        };

    /**
     * Helper that actually processes incoming events.
     * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
     */
    private class Processor
        implements Runnable
    {
        /** loops getting the events **/
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }

                synchronized (mLock) {
                    if (mPaused) {
                        continue;
                    }

                    boolean toHead = true; // were events added to head
                    boolean needUpdate = false;
                    final Iterator it = mPendingEvents.iterator();
                    while (it.hasNext()) {
                        final EventDetails event = (EventDetails) it.next();
                        mAllEvents.add(event);
                        toHead = toHead && (event == mAllEvents.first());
                        needUpdate = needUpdate || matchFilter(event);
                    }
                    mPendingEvents.clear();

                    if (needUpdate) {
                        updateFilteredEvents(toHead);
                    }
                }
            }

        }
    }


    /** names of the columns in the table **/
    private static final String[] COL_NAMES = {
        "Time", "Priority", "Trace", "Category", "NDC", "Message"};

    /** definition of an empty list **/
    private static final EventDetails[] EMPTY_LIST =  new EventDetails[] {};

    /** used to format dates **/
    private static final DateFormat DATE_FORMATTER =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    /** the lock to control access **/
    private final Object mLock = new Object();
    /** set of all logged events - not filtered **/
    private final SortedSet mAllEvents = new TreeSet(MY_COMP);
    /** events that are visible after filtering **/
    private EventDetails[] mFilteredEvents = EMPTY_LIST;
    /** list of events that are buffered for processing **/
    private final List mPendingEvents = new ArrayList();
    /** indicates whether event collection is paused to the UI **/
    private boolean mPaused = false;

    /** filter for the thread **/
    private String mThreadFilter = "";
    /** filter for the message **/
    private String mMessageFilter = "";
    /** filter for the NDC **/
    private String mNDCFilter = "";
    /** filter for the category **/
    private String mCategoryFilter = "";
    /** filter for the priority **/
    private Priority mPriorityFilter = Priority.DEBUG;


    /**
     * Creates a new <code>MyTableModel</code> instance.
     *
     */
    MyTableModel() {
        final Thread t = new Thread(new Processor());
        t.setDaemon(true);
        t.start();
    }


    ////////////////////////////////////////////////////////////////////////////
    // Table Methods
    ////////////////////////////////////////////////////////////////////////////

    /** @see javax.swing.table.TableModel **/
    public int getRowCount() {
        synchronized (mLock) {
            return mFilteredEvents.length;
        }
    }

    /** @see javax.swing.table.TableModel **/
    public int getColumnCount() {
        // does not need to be synchronized
        return COL_NAMES.length;
    }

    /** @see javax.swing.table.TableModel **/
    public String getColumnName(int aCol) {
        // does not need to be synchronized
        return COL_NAMES[aCol];
    }

    /** @see javax.swing.table.TableModel **/
    public Class getColumnClass(int aCol) {
        // does not need to be synchronized
        return (aCol == 2) ? Boolean.class : Object.class;
    }

    /** @see javax.swing.table.TableModel **/
    public Object getValueAt(int aRow, int aCol) {
        synchronized (mLock) {
            final EventDetails event = mFilteredEvents[aRow];

            if (aCol == 0) {
                return DATE_FORMATTER.format(new Date(event.getTimeStamp()));
            } else if (aCol == 1) {
                return event.getPriority();
            } else if (aCol == 2) {
                return (event.getThrowableStrRep() == null)
                    ? Boolean.FALSE : Boolean.TRUE;
            } else if (aCol == 3) {
                return event.getCategoryName();
            } else if (aCol == 4) {
                return event.getNDC();
            }
            return event.getMessage();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Public Methods
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Sets the priority to filter events on. Only events of equal or higher
     * property are now displayed.
     *
     * @param aPriority the priority to filter on
     */
    public void setPriorityFilter(Priority aPriority) {
        synchronized (mLock) {
            mPriorityFilter = aPriority;
            updateFilteredEvents(false);
        }
    }

    /**
     * Set the filter for the thread field.
     *
     * @param aStr the string to match
     */
    public void setThreadFilter(String aStr) {
        synchronized (mLock) {
            mThreadFilter = aStr.trim();
            updateFilteredEvents(false);
        }
    }

    /**
     * Set the filter for the message field.
     *
     * @param aStr the string to match
     */
    public void setMessageFilter(String aStr) {
        synchronized (mLock) {
            mMessageFilter = aStr.trim();
            updateFilteredEvents(false);
        }
    }

    /**
     * Set the filter for the NDC field.
     *
     * @param aStr the string to match
     */
    public void setNDCFilter(String aStr) {
        synchronized (mLock) {
            mNDCFilter = aStr.trim();
            updateFilteredEvents(false);
        }
    }

    /**
     * Set the filter for the category field.
     *
     * @param aStr the string to match
     */
    public void setCategoryFilter(String aStr) {
        synchronized (mLock) {
            mCategoryFilter = aStr.trim();
            updateFilteredEvents(false);
        }
    }

    /**
     * Add an event to the list.
     *
     * @param aEvent a <code>EventDetails</code> value
     */
    public void addEvent(EventDetails aEvent) {
        synchronized (mLock) {
            mPendingEvents.add(aEvent);
        }
    }

    /**
     * Clear the list of all events.
     */
    public void clear() {
        synchronized (mLock) {
            mAllEvents.clear();
            mFilteredEvents = new EventDetails[0];
            mPendingEvents.clear();
            fireTableDataChanged();
        }
    }

    /** Toggle whether collecting events **/
    public void toggle() {
        synchronized (mLock) {
            mPaused = !mPaused;
        }
    }

    /** @return whether currently paused collecting events **/
    public boolean isPaused() {
        synchronized (mLock) {
            return mPaused;
        }
    }

    /**
     * Get the throwable information at a specified row in the filtered events.
     *
     * @param aRow the row index of the event
     * @return the throwable information
     */
    public EventDetails getEventDetails(int aRow) {
        synchronized (mLock) {
            return mFilteredEvents[aRow];
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Update the filtered events data structure.
     * @param aInsertedToFront indicates whether events were added to front of
     *        the events. If true, then the current first event must still exist
     *        in the list after the filter is applied.
     */
    private void updateFilteredEvents(boolean aInsertedToFront) {
        final long start = System.currentTimeMillis();
        final List filtered = new ArrayList();
        final int size = mAllEvents.size();
        final Iterator it = mAllEvents.iterator();

        while (it.hasNext()) {
            final EventDetails event = (EventDetails) it.next();
            if (matchFilter(event)) {
                filtered.add(event);
            }
        }

        final EventDetails lastFirst = (mFilteredEvents.length == 0)
            ? null
            : mFilteredEvents[0];
        mFilteredEvents = (EventDetails[]) filtered.toArray(EMPTY_LIST);

        if (aInsertedToFront && (lastFirst != null)) {
            final int index = filtered.indexOf(lastFirst);
            if (index < 1) {
                LOG.warn("In strange state");
                fireTableDataChanged();
            } else {
                fireTableRowsInserted(0, index - 1);
            }
        } else {
            fireTableDataChanged();
        }

        final long end = System.currentTimeMillis();
        LOG.debug("Total time [ms]: " + (end - start)
                  + " in update, size: " + size);
    }

    /**
     * Returns whether an event matches the filters.
     *
     * @param aEvent the event to check for a match
     * @return whether the event matches
     */
    private boolean matchFilter(EventDetails aEvent) {
        if (aEvent.getPriority().isGreaterOrEqual(mPriorityFilter) &&
            (aEvent.getThreadName().indexOf(mThreadFilter) >= 0) &&
            (aEvent.getCategoryName().indexOf(mCategoryFilter) >= 0) &&
            ((mNDCFilter.length() == 0) ||
             ((aEvent.getNDC() != null) &&
              (aEvent.getNDC().indexOf(mNDCFilter) >= 0))))
        {
            final String rm = aEvent.getMessage();
            if (rm == null) {
                // only match if we have not filtering in place
                return (mMessageFilter.length() == 0);
            } else {
                return (rm.indexOf(mMessageFilter) >= 0);
            }
        }

        return false; // by default not match
    }
}
