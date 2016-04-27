package org.opennms.netmgt.poller;

import java.util.Map;

import org.apache.camel.InOnly;
import org.apache.camel.Produce;
import org.opennms.core.camel.DefaultDispatcher;
import org.opennms.netmgt.passive.PassiveStatusKeeper;

/**
 * This class is an {@link InOnly} endpoint that will send messages to the 
 * Camel endpoint specified by the <code>endpointUri</code> constructor argument.
 */
@InOnly
public class ServiceMonitorCamelImpl extends DefaultDispatcher implements ServiceMonitor{
	
	@Produce(property="endpointUri")
	ServiceMonitor serviceMonitor;
	
	public ServiceMonitorCamelImpl(String endpointUri) {
		super(endpointUri);
	}

	@Override
	public void close() {
		serviceMonitor.close();
	}

	@Override
	public PollStatus poll(MonitoredServiceTask monSvct) {
		return serviceMonitor.poll(monSvct);
	}

}
