/*
 * *************************************************************************************
 *  Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 *  http://esper.codehaus.org                                                          *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationState;

import java.util.Collection;

public class SupportAggMFAccessorPlainScalar implements AggregationAccessor {
    public Object getValue(AggregationState state) {
        return ((SupportAggMFStatePlainScalar) state).getLastValue();
    }

    public Collection<EventBean> getEnumerableEvents(AggregationState state) {
        return null;
    }

    public EventBean getEnumerableEvent(AggregationState state) {
        return null;
    }
}
