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

package com.espertech.esperio.dataflow;

import com.espertech.esper.client.EPException;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esperio.AdapterInputSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

public class FileSourceFactory implements DataFlowOperatorFactory {

    private static final Log log = LogFactory.getLog(FileSourceFactory.class);

    @DataFlowOpParameter
    private String file;

    @DataFlowOpParameter
    private boolean classpathFile;

    @DataFlowOpParameter
    private boolean hasHeaderLine;

    @DataFlowOpParameter
    private boolean hasTitleLine;

    @DataFlowOpParameter
    private AdapterInputSource adapterInputSource;

    @DataFlowOpParameter
    private Integer numLoops;

    @DataFlowOpParameter
    private String[] propertyNames;

    @DataFlowOpParameter
    private String format;

    @DataFlowOpParameter
    private String propertyNameLine;

    @DataFlowOpParameter
    private String propertyNameFile;

    @DataFlowOpParameter
    private String dateFormat;

    public DataFlowSourceOperator create() {

        AdapterInputSource inputSource;
        if (adapterInputSource != null) {
            inputSource = adapterInputSource;
        }
        else if (file != null) {
            if (classpathFile) {
                inputSource = new AdapterInputSource(file);
            }
            else {
                inputSource = new AdapterInputSource(new File(file));
            }
        }
        else {
            throw new EPException("Failed to find required parameter, either the file or the adapterInputSource parameter is required");
        }

        if (format == null || format.equals("csv")) {
            return new FileSourceCSV(inputSource, hasHeaderLine, hasTitleLine, numLoops, propertyNames, dateFormat);
        }
        else if (format.equals("line")) {
            return new FileSourceLineUnformatted(inputSource, file, propertyNameLine, propertyNameFile);
        }
        else {
            throw new IllegalArgumentException("Unrecognized file format '" + format + "'");
        }
    }
}
