/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2015-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.trapd;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.config.TrapdConfig;
import org.opennms.netmgt.snmp.TrapNotification;
import org.opennms.netmgt.snmp.TrapProcessor;

/**
 * This processor will update the {@link SyslogdConfig} on a
 * {@link SyslogConnection} so that it can be processed according
 * to the new configuration.
 * 
 * @author Seth
 */
public class TrapdConfigProcessor {

	private final TrapdConfig m_config;

	public TrapdConfigProcessor(TrapdConfig config) {
		m_config = config;
	}

	public TrapNotification process(TrapQueueProcessor connection) {
		System.out.println("------------------------------------------------");
		if(connection == null)
			connection = new TrapQueueProcessor();
		
		/**
		 * This as done just for testing purpose. Need to remove once testing is complete
		 */
		
		TrapProcessor trapProcess = new TrapProcessorImpl();
		trapProcess.setAgentAddress(InetAddressUtils.ONE_TWENTY_SEVEN);
		trapProcess.setCommunity("comm");
		trapProcess.setTimeStamp(System.currentTimeMillis());
		trapProcess.setTrapAddress(InetAddressUtils.ONE_TWENTY_SEVEN);
		
		
		connection.setTrapNotification(new TrapNotificationImpl(trapProcess));
		
		connection.setNewSuspect(false);
		return connection.getTrapNotification();
	}
}
