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
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.event.SupportEventAdapterService;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;

public class TestKeyedFastPropertyGetter extends TestCase
{
    private KeyedFastPropertyGetter getter;
    private EventBean theEvent;
    private SupportBeanComplexProps bean;

    public void setUp() throws Exception
    {
        bean = SupportBeanComplexProps.makeDefaultBean();
        theEvent = SupportEventBeanFactory.createObject(bean);
        FastClass fastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), SupportBeanComplexProps.class);
        FastMethod method = fastClass.getMethod("getIndexed", new Class[] {int.class});
        getter = new KeyedFastPropertyGetter(method, 1, SupportEventAdapterService.getService());
    }

    public void testGet()
    {
        assertEquals(bean.getIndexed(1), getter.get(theEvent));

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
