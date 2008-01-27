package com.espertech.esper.view.window;

import com.espertech.esper.event.EventBean;

import java.util.Map;
import java.util.HashMap;

/**
 * Provides random-access into window contents by event and index as a combination. 
 */
public class RelativeAccessByEventNIndexGetter implements IStreamRelativeAccess.IStreamRelativeAccessUpdateObserver
{
    private final Map<EventBean, IStreamRelativeAccess> accessorByEvent = new HashMap<EventBean, IStreamRelativeAccess>();
    private final Map<IStreamRelativeAccess, EventBean[]> eventsByAccessor  = new HashMap<IStreamRelativeAccess, EventBean[]>();

    public void updated(IStreamRelativeAccess iStreamRelativeAccess, EventBean[] newData)
    {
        // remove data posted from the last update
        EventBean[] lastNewData = eventsByAccessor.get(iStreamRelativeAccess);
        if (lastNewData != null)
        {
            for (int i = 0; i < lastNewData.length; i++)
            {
                accessorByEvent.remove(lastNewData[i]);
            }
        }

        if (newData == null)
        {
            return;
        }

        // hold accessor per event for querying
        for (int i = 0; i < newData.length; i++)
        {
            accessorByEvent.put(newData[i], iStreamRelativeAccess);
        }

        // save new data for access to later removal
        eventsByAccessor.put(iStreamRelativeAccess, newData);
    }

    /**
     * Returns the access into window contents given an event.
     * @param event to which the method returns relative access from
     * @return buffer
     */
    public IStreamRelativeAccess getAccessor(EventBean event)
    {
        IStreamRelativeAccess iStreamRelativeAccess = accessorByEvent.get(event);
        if (iStreamRelativeAccess == null)
        {
            throw new IllegalStateException("Accessor for window random access not found for event " + event);
        }
        return iStreamRelativeAccess;
    }
}