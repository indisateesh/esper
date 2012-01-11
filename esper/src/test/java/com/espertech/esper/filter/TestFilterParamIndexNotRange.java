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

package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.filter.SupportEventEvaluator;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestFilterParamIndexNotRange extends TestCase
{
    private SupportEventEvaluator testEvaluators[];
    private SupportBean testBean;
    private EventBean testEventBean;
    private EventType testEventType;
    private List<FilterHandle> matchesList;

    public void setUp()
    {
        testEvaluators = new SupportEventEvaluator[4];
        for (int i = 0; i < testEvaluators.length; i++)
        {
            testEvaluators[i] = new SupportEventEvaluator();
        }

        testBean = new SupportBean();
        testEventBean = SupportEventBeanFactory.createObject(testBean);
        testEventType = testEventBean.getEventType();
        matchesList = new LinkedList<FilterHandle>();
    }

    public void testClosedRange()
    {
        FilterParamIndexDoubleRangeInverted index = makeOne("longBoxed", FilterOperator.NOT_RANGE_CLOSED, testEventType);
        assertEquals(FilterOperator.NOT_RANGE_CLOSED, index.getFilterOperator());

        index.put(new DoubleRange(2d, 4d), testEvaluators[0]);
        index.put(new DoubleRange(2d, 5d), testEvaluators[1]);
        index.put(new DoubleRange(1d, 3d), testEvaluators[2]);
        index.put(new DoubleRange(1d, 1d), testEvaluators[3]);

        verify(index, 0L, new boolean[] {true, true, true, true});
        verify(index, 1L, new boolean[] {true, true, false, false});
        verify(index, 2L, new boolean[] {false, false, false, true});
        verify(index, 3L, new boolean[] {false, false, false, true});
        verify(index, 4L, new boolean[] {false, false, true, true});
        verify(index, 5L, new boolean[] {true, false, true, true});
        verify(index, 6L, new boolean[] {true, true, true, true});
    }

    public void testOpenRange()
    {
        FilterParamIndexDoubleRangeInverted index = makeOne("longBoxed", FilterOperator.NOT_RANGE_OPEN, testEventType);

        index.put(new DoubleRange(2d, 4d), testEvaluators[0]);
        index.put(new DoubleRange(2d, 5d), testEvaluators[1]);
        index.put(new DoubleRange(1d, 3d), testEvaluators[2]);
        index.put(new DoubleRange(1d, 1d), testEvaluators[3]);

        verify(index, 0L, new boolean[] {true, true, true, true});
        verify(index, 1L, new boolean[] {true, true, true, true});
        verify(index, 2L, new boolean[] {true, true, false, true});
        verify(index, 3L, new boolean[] {false, false, true, true});
        verify(index, 4L, new boolean[] {true, false, true, true});
        verify(index, 5L, new boolean[] {true, true, true, true});
        verify(index, 6L, new boolean[] {true, true, true, true});
    }

    public void testHalfOpenRange()
    {
        FilterParamIndexDoubleRangeInverted index = makeOne("longBoxed", FilterOperator.NOT_RANGE_HALF_OPEN, testEventType);

        index.put(new DoubleRange(2d, 4d), testEvaluators[0]);
        index.put(new DoubleRange(2d, 5d), testEvaluators[1]);
        index.put(new DoubleRange(1d, 3d), testEvaluators[2]);
        index.put(new DoubleRange(1d, 1d), testEvaluators[3]);

        verify(index, 0L, new boolean[] {true, true, true, true});
        verify(index, 1L, new boolean[] {true, true, false, true});
        verify(index, 2L, new boolean[] {false, false, false, true});
        verify(index, 3L, new boolean[] {false, false, true, true});
        verify(index, 4L, new boolean[] {true, false, true, true});
        verify(index, 5L, new boolean[] {true, true, true, true});
        verify(index, 6L, new boolean[] {true, true, true, true});
    }

    public void testHalfClosedRange()
    {
        FilterParamIndexDoubleRangeInverted index = makeOne("longBoxed", FilterOperator.NOT_RANGE_HALF_CLOSED, testEventType);

        index.put(new DoubleRange(2d, 4d), testEvaluators[0]);
        index.put(new DoubleRange(2d, 5d), testEvaluators[1]);
        index.put(new DoubleRange(1d, 3d), testEvaluators[2]);
        index.put(new DoubleRange(1d, 1d), testEvaluators[3]);

        verify(index, 0L, new boolean[] {true, true, true, true});
        verify(index, 1L, new boolean[] {true, true, true, true});
        verify(index, 2L, new boolean[] {true, true, false, true});
        verify(index, 3L, new boolean[] {false, false, false, true});
        verify(index, 4L, new boolean[] {false, false, true, true});
        verify(index, 5L, new boolean[] {true, false, true, true});
        verify(index, 6L, new boolean[] {true, true, true, true});
    }

    private FilterParamIndexDoubleRangeInverted makeOne(String field, FilterOperator notRangeHalfClosed, EventType testEventType) {
        return new FilterParamIndexDoubleRangeInverted(makeLookupable(field), notRangeHalfClosed);
    }

    private void verify(FilterParamIndexBase index, Long testValue, boolean[] expected)
    {
        testBean.setLongBoxed(testValue);
        index.matchEvent(testEventBean, matchesList);
        for (int i = 0; i < expected.length; i++)
        {
            assertEquals("Unexpected result for eval " + i, expected[i], testEvaluators[i].getAndResetCountInvoked() == 1);
        }
    }

    private FilterSpecLookupable makeLookupable(String fieldName) {
        return new FilterSpecLookupable(fieldName, testEventType.getGetter(fieldName), testEventType.getPropertyType(fieldName));
    }
}
