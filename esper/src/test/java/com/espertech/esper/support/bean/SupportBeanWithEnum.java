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

package com.espertech.esper.support.bean;

import java.io.Serializable;

public class SupportBeanWithEnum implements Serializable
{
    private String theString;
    private SupportEnum supportEnum;

    public SupportBeanWithEnum(String theString, SupportEnum supportEnum)
    {
        this.theString = theString;
        this.supportEnum = supportEnum;
    }

    public String getTheString()
    {
        return theString;
    }

    public SupportEnum getSupportEnum()
    {
        return supportEnum;
    }
}
