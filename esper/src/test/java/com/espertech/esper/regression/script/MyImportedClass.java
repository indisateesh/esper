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

package com.espertech.esper.regression.script;

public class MyImportedClass {
    public final static String VALUE_P00 = "VALUE_P00";
    private final String p00 = VALUE_P00;

    public MyImportedClass() {
    }

    public String getP00() {
        return p00;
    }
}
