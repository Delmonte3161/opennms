/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
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

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.netmgt.snmp.TrapNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/META-INF/opennms/emptyContext.xml" })
public class TrapdListenerBlueprintIT extends CamelBlueprintTestSupport {

	private static final Logger LOG = LoggerFactory.getLogger(TrapdListenerBlueprintIT.class);
	
	private static final String PORT_NAME="trapd.listen.port";
	
	private static final String PERSISTANCE_ID="org.opennms.netmgt.trapd";

	/**
	 * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
	 * 
	 * @see https://issues.apache.org/jira/browse/ARIES-1051
	 * @see https://access.redhat.com/site/solutions/640943
	 */
	@Override
	public void doPreSetup() throws Exception {
		System.setProperty("org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString());
		System.setProperty("de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString());
	}

	@Override
	public boolean isUseAdviceWith() {
		return true;
	}

	@Override
	public boolean isUseDebugger() {
		// must enable debugger
		return true;
	}

	@Override
	public String isMockEndpoints() {
		return "*";
	}

	/**
	 * This method overrides the blueprint property and sets port to 10514 instead of 162
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	protected String useOverridePropertiesWithConfigAdmin(Dictionary props) throws Exception {
		props.put(PORT_NAME, 10514);
		return PERSISTANCE_ID;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Register any mock OSGi services here
		services.put(TrapNotificationHandler.class.getName(), new KeyValueHolder<Object, Dictionary>(new TrapNotificationHandler() {

			@Override
			public void handleTrapNotification(TrapNotification message) {
				// TODO Auto-generated method stub
				
			}

		}, new Properties()));
	}

	// The location of our Blueprint XML files to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:blueprint-trapd-listener.xml,file:src/test/resources/blueprint-empty-camel-context.xml";
	}

	@Test
	public void testTrapd() throws Exception {
		// TODO: Perform integration testing
	}
}