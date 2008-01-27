package com.espertech.esper.eql.spec;

import com.espertech.esper.util.MetaDefItem;

import java.util.List;

/**
 * Specification for creating a named window.
 */
public class CreateWindowDesc implements MetaDefItem
{
    private String windowName;
    private List<ViewSpec> viewSpecs;

    /**
     * Ctor.
     * @param windowName the window name
     * @param viewSpecs the view definitions
     */
    public CreateWindowDesc(String windowName, List<ViewSpec> viewSpecs)
    {
        this.windowName = windowName;
        this.viewSpecs = viewSpecs;
    }

    /**
     * Returns the window name.
     * @return window name
     */
    public String getWindowName()
    {
        return windowName;
    }

    /**
     * Returns the view specifications.
     * @return view specs
     */
    public List<ViewSpec> getViewSpecs()
    {
        return viewSpecs;
    }
}
