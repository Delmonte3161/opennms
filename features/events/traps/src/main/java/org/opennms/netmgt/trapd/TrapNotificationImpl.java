/**
 * 
 */
package org.opennms.netmgt.trapd;

import org.opennms.netmgt.snmp.TrapNotification;
import org.opennms.netmgt.snmp.TrapProcessor;

/**
 * @author pk015603
 *
 */
public class TrapNotificationImpl implements TrapNotification{
	
	
	public TrapNotificationImpl(TrapProcessor processor) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public TrapProcessor getTrapProcessor() {
		TrapProcessor processor = new TrapProcessorImpl();
		
		return processor;
	}

}
