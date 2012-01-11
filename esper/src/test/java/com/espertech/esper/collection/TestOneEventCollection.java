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

package com.espertech.esper.collection;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.client.EventBean;

public class TestOneEventCollection extends TestCase
{
    private OneEventCollection list;
    private EventBean[] events;

    public void setUp()
    {
        list = new OneEventCollection();
        events = SupportEventBeanFactory.makeEvents(new String[] {"1", "2", "3", "4"});
    }

    public void testFlow()
    {
        assertTrue(list.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(list.toArray(), new EventBean[0]);

        list.add(events[0]);
        assertFalse(list.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(list.toArray(), new EventBean[]{events[0]});

        list.add(events[1]);
        assertFalse(list.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(list.toArray(), new EventBean[]{events[0], events[1]});

        list.add(events[2]);
        assertFalse(list.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(list.toArray(), new EventBean[]{events[0], events[1], events[2]});
    }
}
