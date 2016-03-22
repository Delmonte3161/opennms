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
	
	
	private TrapProcessor trapProcessor;

	public TrapNotificationImpl(TrapProcessor trapProcess) {
		this.trapProcessor=trapProcess;
	}

	@Override
	public TrapProcessor getTrapProcessor() {
		 return trapProcessor;
		
	}

}
