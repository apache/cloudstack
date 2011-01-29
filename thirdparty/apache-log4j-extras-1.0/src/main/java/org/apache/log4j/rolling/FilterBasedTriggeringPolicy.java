/*
 * Copyright 1999,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.rolling;

import org.apache.log4j.Appender;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.spi.TriggeringEventEvaluator;
import org.apache.log4j.xml.UnrecognizedElementHandler;
import org.w3c.dom.Element;

import java.util.Properties;


/**
 * FilterBasedTriggeringPolicy determines if rolling should be triggered
 * by evaluating the current message against a set of filters.  Unless a
 * filter rejects a message, a rolling event will be triggered.
 *
 * @author Curt Arnold
 *
 */
public final class FilterBasedTriggeringPolicy
        implements TriggeringPolicy, TriggeringEventEvaluator, UnrecognizedElementHandler {
  /**
   * The first filter in the filter chain. Set to <code>null</code> initially.
   */
  private Filter headFilter;

  /**
   * The last filter in the filter chain.
   */
  private Filter tailFilter;

  /**
   *  Creates a new FilterBasedTriggeringPolicy.
   */
  public FilterBasedTriggeringPolicy() {
  }

    /**
     * {@inheritDoc}
     */
  public boolean isTriggeringEvent(LoggingEvent event) {
    //
    //   in the abnormal case of no contained filters
    //     always return true to avoid each logging event
    //     from having its own file.
    if (headFilter == null) {
      return false;
    }

    //
    //    otherwise loop through the filters
    //
    for (Filter f = headFilter; f != null; f = f.next) {
      switch (f.decide(event)) {
      case Filter.DENY:
        return false;

      case Filter.ACCEPT:
        return true;
      }
    }

    return true;
   }


  /**
   * {@inheritDoc}
   *
   */
  public boolean isTriggeringEvent(
    final Appender appender, final LoggingEvent event, final String file,
    final long fileLength) {
    return isTriggeringEvent(event);
  }

  /**
   * Add a filter to end of the filter list.
   * @param newFilter filter to add to end of list.
   */
  public void addFilter(final Filter newFilter) {
    if (headFilter == null) {
      headFilter = newFilter;
      tailFilter = newFilter;
    } else {
      tailFilter.next = newFilter;
      tailFilter = newFilter;
    }
  }

  /**
   * Clear the filters chain.
   *
   */
  public void clearFilters() {
    headFilter = null;
    tailFilter = null;
  }

  /**
   * Returns the head Filter.
   * @return head of filter chain, may be null.
   *
   */
  public Filter getFilter() {
    return headFilter;
  }

  /**
   *  {@inheritDoc}
   */
  public void activateOptions() {
    for (Filter f = headFilter; f != null; f = f.next) {
      f.activateOptions();
    }
  }

    /**
     * {@inheritDoc}
     */
  public boolean parseUnrecognizedElement(final Element element,
                                          final Properties props) throws Exception {
      final String nodeName = element.getNodeName();
      if ("filter".equals(nodeName)) {
          OptionHandler filter =
                  org.apache.log4j.extras.DOMConfigurator.parseElement(
                          element, props, Filter.class);
          if (filter instanceof Filter) {
              filter.activateOptions();
              this.addFilter((Filter) filter);
          }
          return true;
      }
      return false;
  }

}
