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

package com.espertech.esper.regression.dataflow;

public class MyWordCountStats {
    private int lines;
    private int words;
    private int chars;

    public MyWordCountStats() {
        lines = 0;
        words = 0;
        chars = 0;
    }

    public int getLines() {
        return lines;
    }

    public int getWords() {
        return words;
    }

    public int getChars() {
        return chars;
    }

    public void add(int lines, int words, int chars) {
        this.lines += lines;
        this.words += words;
        this.chars += chars;
    }

    public String toString() {
        return "WordCountStats{" +
                "lines=" + lines +
                ", words=" + words +
                ", chars=" + chars +
                '}';
    }
}
