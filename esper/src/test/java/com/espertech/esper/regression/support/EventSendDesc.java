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

package com.espertech.esper.regression.support;

import com.espertech.esper.support.bean.SupportMarketDataBean;

public class EventSendDesc
{
    private SupportMarketDataBean theEvent;
    private String eventDesc;

    public EventSendDesc(SupportMarketDataBean theEvent, String eventDesc) {
        this.theEvent = theEvent;
        this.eventDesc = eventDesc;
    }

    public SupportMarketDataBean getTheEvent() {
        return theEvent;
    }

    public String getEventDesc() {
        return eventDesc;
    }
}
