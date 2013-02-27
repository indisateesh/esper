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
import com.espertech.esper.pattern.MatchedEventMap;
import com.espertech.esper.pattern.MatchedEventMapImpl;
import com.espertech.esper.pattern.MatchedEventMapMeta;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.event.SupportEventAdapterService;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.filter.SupportFilterSpecBuilder;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;
import junit.framework.TestCase;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class TestFilterSpecCompiled extends TestCase
{
    private EventType eventType;
    private String eventTypeName;

    public void setUp()
    {
        eventTypeName = SupportBean.class.getName();
        eventType = SupportEventAdapterService.getService().addBeanType(eventTypeName, SupportBean.class, true, true, true);
    }

    public void testEquals()
    {
        Object[][] paramList = new Object[][] {
            { "intPrimitive", FilterOperator.EQUAL, 2, "intBoxed", FilterOperator.EQUAL, 3 },
            { "intPrimitive", FilterOperator.EQUAL, 3, "intBoxed", FilterOperator.EQUAL, 3 },
            { "intPrimitive", FilterOperator.EQUAL, 2 },
            { "intPrimitive", FilterOperator.RANGE_CLOSED, 1, 10 },
            { "intPrimitive", FilterOperator.EQUAL, 2, "intBoxed", FilterOperator.EQUAL, 3 },
            { },
            { },
            };

        Vector<FilterSpecCompiled> specVec = new Vector<FilterSpecCompiled>();
        for (Object[] param : paramList)
        {
            FilterSpecCompiled spec = SupportFilterSpecBuilder.build(eventType, param);
            specVec.add(spec);
        }

        assertFalse(specVec.get(0).equals(specVec.get(1)));
        assertFalse(specVec.get(0).equals(specVec.get(2)));
        assertFalse(specVec.get(0).equals(specVec.get(3)));
        assertEquals(specVec.get(0), specVec.get(4));
        assertFalse(specVec.get(0).equals(specVec.get(5)));
        assertEquals(specVec.get(5), specVec.get(6));

        assertFalse(specVec.get(2).equals(specVec.get(4)));
    }

    public void testGetValueSet()
    {
        List<FilterSpecParam> parameters = SupportFilterSpecBuilder.buildList(eventType, new Object[]
                                    { "intPrimitive", FilterOperator.EQUAL, 2 });
        SimpleNumberCoercer numberCoercer = SimpleNumberCoercerFactory.getCoercer(int.class, Double.class);
        parameters.add(new FilterSpecParamEventProp(makeLookupable("doubleBoxed"), FilterOperator.EQUAL, "asName", "doublePrimitive", false, numberCoercer, Double.class, "Test"));
        FilterSpecCompiled filterSpec = new FilterSpecCompiled(eventType, "SupportBean", parameters, null);

        SupportBean eventBean = new SupportBean();
        eventBean.setDoublePrimitive(999.999);
        EventBean theEvent = SupportEventBeanFactory.createObject(eventBean);
        MatchedEventMap matchedEvents = new MatchedEventMapImpl(new MatchedEventMapMeta(new String[] {"asName"}, false));
        matchedEvents.add(0, theEvent);
        FilterValueSet valueSet = filterSpec.getValueSet(matchedEvents, null, null);

        // Assert the generated filter value container
        assertSame(eventType, valueSet.getEventType());
        assertEquals(2, valueSet.getParameters().length);

        // Assert the first param
        FilterValueSetParam param = valueSet.getParameters()[0];
        assertEquals("intPrimitive", param.getLookupable().getExpression());
        assertEquals(FilterOperator.EQUAL, param.getFilterOperator());
        assertEquals(2, param.getFilterForValue());

        // Assert the second param
        param = (FilterValueSetParam) valueSet.getParameters()[1];
        assertEquals("doubleBoxed", param.getLookupable().getExpression());
        assertEquals(FilterOperator.EQUAL, param.getFilterOperator());
        assertEquals(999.999, param.getFilterForValue());
    }

    private FilterSpecLookupable makeLookupable(String fieldName) {
        return new FilterSpecLookupable(fieldName, eventType.getGetter(fieldName), eventType.getPropertyType(fieldName));
    }

    public void testPresortParameters()
    {
        FilterSpecCompiled spec = makeFilterValues(
                "doublePrimitive", FilterOperator.LESS, 1.1,
                "doubleBoxed", FilterOperator.LESS, 1.1,
                "intPrimitive", FilterOperator.EQUAL, 1,
                "string", FilterOperator.EQUAL, "jack",
                "intBoxed", FilterOperator.EQUAL, 2,
                "floatBoxed", FilterOperator.RANGE_CLOSED, 1.1d, 2.2d);

        ArrayDeque<FilterSpecParam> copy = new ArrayDeque<FilterSpecParam>(Arrays.asList(spec.getParameters()));

        assertEquals("intPrimitive", copy.remove().getLookupable().getExpression());
        assertEquals("string", copy.remove().getLookupable().getExpression());
        assertEquals("intBoxed", copy.remove().getLookupable().getExpression());
        assertEquals("floatBoxed", copy.remove().getLookupable().getExpression());
        assertEquals("doublePrimitive", copy.remove().getLookupable().getExpression());
        assertEquals("doubleBoxed", copy.remove().getLookupable().getExpression());
    }

    private FilterSpecCompiled makeFilterValues(Object ... filterSpecArgs)
    {
        return SupportFilterSpecBuilder.build(eventType, filterSpecArgs);
    }
}
