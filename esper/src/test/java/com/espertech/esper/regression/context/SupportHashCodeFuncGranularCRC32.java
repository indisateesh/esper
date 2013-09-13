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

package com.espertech.esper.regression.context;

import java.util.zip.CRC32;

public class SupportHashCodeFuncGranularCRC32 implements TestContextHashSegmented.HashCodeFunc {
    private int granularity;

    public SupportHashCodeFuncGranularCRC32(int granularity) {
        this.granularity = granularity;
    }

    public int codeFor(String key) {
        long codeMod = computeCRC32(key) % granularity;
        return (int) codeMod;
    }

    public static long computeCRC32(String key) {
        CRC32 crc = new CRC32();
        crc.update(key.getBytes());
        return crc.getValue();
    }
}
