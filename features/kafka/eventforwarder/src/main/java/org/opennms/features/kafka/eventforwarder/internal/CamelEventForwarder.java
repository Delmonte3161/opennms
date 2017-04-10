package org.opennms.features.kafka.eventforwarder.internal;

import org.apache.camel.InOnly;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;

@InOnly
public interface CamelEventForwarder extends EventForwarder
{
    /**
     * Called by a service to send an event to eventd
     *
     * @param event a {@link org.opennms.netmgt.xml.event.Event} object.
     */
    @Override
    void sendNow( Event event );

    /**
     * Called by a service to send a set of events to eventd
     *
     * @param eventLog a {@link org.opennms.netmgt.xml.event.Log} object.
     */
    @Override
    void sendNow( Log eventLog );
}
