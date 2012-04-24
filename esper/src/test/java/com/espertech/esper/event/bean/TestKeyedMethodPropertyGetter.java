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

package com.espertech.esper.event.bean;

import junit.framework.TestCase;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.event.SupportEventAdapterService;

import java.lang.reflect.Method;

public class TestKeyedMethodPropertyGetter extends TestCase
{
    private KeyedMethodPropertyGetter getter;
    private EventBean theEvent;
    private SupportBeanComplexProps bean;

    public void setUp() throws Exception
    {
        bean = SupportBeanComplexProps.makeDefaultBean();
        theEvent = SupportEventBeanFactory.createObject(bean);
        Method method = SupportBeanComplexProps.class.getMethod("getIndexed", new Class[] {int.class});
        getter = new KeyedMethodPropertyGetter(method, 1, SupportEventAdapterService.getService());
    }

    public void testGet()
    {
        assertEquals(bean.getIndexed(1), getter.get(theEvent));
        assertEquals(bean.getIndexed(1), getter.get(theEvent, 1));

        try
        {
            getter.get(SupportEventBeanFactory.createObject(""));
            fail();
        }
        catch (PropertyAccessException ex)
        {
            // expected
        }
    }
}
