/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
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
package org.opennms.smoketest.minion;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpV1TrapBuilder;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.SnmpValueFactory;
import org.opennms.test.system.api.NewTestEnvironment.ContainerAlias;
import org.opennms.test.system.api.TestEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that SNMP traps sent to the Minion generate
 * events in OpenNMS.
 *
 * @author seth
 */
public class TrapTest extends AbstractSyslogTestCase {
    private static final Logger LOG = LoggerFactory.getLogger(TrapTest.class);

    @Override
    protected TestEnvironmentBuilder getEnvironmentBuilder() {
        TestEnvironmentBuilder builder = super.getEnvironmentBuilder();
        return builder;
    }


    @Test
    public void canReceiveTraps() throws Exception {

    	ContainerAlias containerAlias = getMinionAlias();
   	
        InetSocketAddress minionSshAddr = testEnvironment.getServiceAddress(containerAlias,8201);
        final InetSocketAddress trapAddr = testEnvironment.getServiceAddress(containerAlias, 162, "udp");
        InetSocketAddress esRestAddr = testEnvironment.getServiceAddress(ContainerAlias.ELASTICSEARCH_5, 9200);
        InetSocketAddress opennmsSshAddr = testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 8101);
        InetSocketAddress kafkaAddress = testEnvironment.getServiceAddress(ContainerAlias.KAFKA, 9092);
        InetSocketAddress zookeeperAddress = testEnvironment.getServiceAddress(ContainerAlias.KAFKA, 2181);

        installFeaturesOnMinion(minionSshAddr, kafkaAddress);
        
        installFeaturesOnOpenNMS(opennmsSshAddr, kafkaAddress, zookeeperAddress);

        for (int i = 0; i < 10; i++) {
            LOG.info("Slept for " + i + " seconds");
            Thread.sleep(1000);
        }
        LOG.info("Resetting statistics");
        resetRouteStatistics(opennmsSshAddr, minionSshAddr);

        
        LOG.info("Start sending traps");
        int resendCount = 0;
        
        
       while(pollForElasticsearchEventsForTraps(esRestAddr,"uei.opennms.org/vendor/Cisco/traps/ciscoC3800CardActive") == 0){
        	resendCount++;
     	   	LOG.info("Resending Packets:"+resendCount);
     	   	sendV1Trap(trapAddr);
     	   	Thread.sleep(60000);
     	   	if(resendCount>30){
     	   		LOG.info("Timed out :( Test failed! ");
     	   		break;
      	   }
        }
        assertTrue(resendCount<30);
        LOG.info("Completed V1 Trap Test");
        
        resendCount = 0;
        while(pollForElasticsearchEventsForTraps(esRestAddr,"uei.opennms.org/vendor/Cisco/traps/c2900AddressViolation") == 0){
        	resendCount++;
     	   	LOG.info("Resending Packets:"+resendCount);
     	   	sendV2Trap(trapAddr);
     	   	Thread.sleep(60000);
     	   	if(resendCount>30){
     	   		LOG.info("Timed out :( Test failed! ");
     	   		break;
      	   }
        }
        
        assertTrue(resendCount<30);
        LOG.info("Completed V2 Trap Test");
        
        /*resendCount = 0;
        while(pollForElasticsearchEventsForTraps(esRestAddr,null) == 0){
        	resendCount++;
     	   	LOG.info("Resending Packets:"+resendCount);
     	   	sendV3Trap(trapAddr);
     	   	Thread.sleep(60000);
     	   	if(resendCount>30){
     	   		LOG.info("Timed out :( Test failed! ");
     	   		break;
      	   }
        }
        
        assertTrue(resendCount<30);
        LOG.info("Completed V3 Trap Test");*/
        
    }

    private void sendV1Trap(final InetSocketAddress trapAddr) {
        LOG.info("Sending v1 trap");
        SnmpV1TrapBuilder pdu = SnmpUtils.getV1TrapBuilder();
		try {
		pdu.setEnterprise(SnmpObjId.get(".1.3.6.1.4.1.9.9.70.2"));
		pdu.setGeneric(6);
		pdu.setSpecific(1);
		pdu.setTimeStamp(0);
		pdu.setAgentAddress(InetAddress.getLocalHost());
		
		Iterator<Map.Entry<String, SnmpValue>> it = getVarBinds().entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, SnmpValue> pairs = it.next();
			pdu.addVarBind(SnmpObjId.get(pairs.getKey()), pairs.getValue());
		}
		
		pdu.send(InetAddressUtils.str(trapAddr.getAddress()), trapAddr.getPort(), "public");
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void sendV2Trap(final InetSocketAddress trapAddr){
        LOG.info("Sending v2 trap");
        try {
        	// Comes as warning
    		SnmpObjId enterpriseId = SnmpObjId.get(".1.3.6.1.4.1.9.9.87.2");
    		boolean isGeneric = false;
    		SnmpObjId trapOID;
    		if ((SnmpObjId.get(".1.3.6.1.4.1.9.10").toString())
    				.contains(enterpriseId.toString())) {
    			isGeneric = true;
    			trapOID = enterpriseId;
    		} else {
    			trapOID = SnmpObjId.get(enterpriseId, new SnmpInstId(1));
    		}

    		SnmpTrapBuilder pdu = SnmpUtils.getV2TrapBuilder();
    		pdu.addVarBind(SnmpObjId.get(".1.3.6.1.2.1.1.3.0"), SnmpUtils
    				.getValueFactory().getTimeTicks(0));
    		pdu.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.1.0"), SnmpUtils
    				.getValueFactory().getObjectId(trapOID));
    		if (isGeneric) {
    			pdu.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.3.0"), SnmpUtils
    					.getValueFactory().getObjectId(enterpriseId));
    		}
    		Iterator<Map.Entry<String, SnmpValue>> it = getVarBinds().entrySet()
    				.iterator();
    		while (it.hasNext()) {
    			Map.Entry<String, SnmpValue> pairs = it.next();
    			pdu.addVarBind(SnmpObjId.get(pairs.getKey()), pairs.getValue());
    		}
            pdu.send(InetAddressUtils.str(trapAddr.getAddress()), trapAddr.getPort(), "public");
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("Trap has been sent");
    }
    /*
     * This method will be enabled when snmpv3 traps are enabled in production
     */
   /* private void sendV3Trap(final InetSocketAddress trapAddr){
        LOG.info("Sending v3 trap");
        try {
            SnmpObjId enterpriseId = SnmpObjId.get(".1.3.6.1.4.1.9.9.87.2");
            SnmpObjId trapOID;
            if ((SnmpObjId.get(".1.3.6.1.4.1.9.10").toString())
                                            .contains(enterpriseId.toString())) {
                            trapOID = enterpriseId;
            } else {
                            trapOID = SnmpObjId.get(enterpriseId, new SnmpInstId(1));
            }
        	
        	 SnmpV3TrapBuilder pduv3 = SnmpUtils.getV3TrapBuilder();
             pduv3.addVarBind(SnmpObjId.get(".1.3.6.1.2.1.1.3.0"), SnmpUtils.getValueFactory().getTimeTicks(0));
             pduv3.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.1.0"), SnmpUtils.getValueFactory().getObjectId(trapOID));
             pduv3.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.3.0"), SnmpUtils.getValueFactory().getObjectId(enterpriseId));
             pduv3.send(InetAddressUtils.str(trapAddr.getAddress()), trapAddr.getPort(), SnmpConfiguration.AUTH_PRIV, "opennmsUser", "0p3nNMSv3", SnmpConfiguration.DEFAULT_AUTH_PROTOCOL, "0p3nNMSv3", SnmpConfiguration.DEFAULT_PRIV_PROTOCOL);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("Trap has been sent");
    	
    }*/
    

	private static LinkedHashMap<String, SnmpValue> getVarBinds() {
		SnmpValueFactory valueFactory = SnmpUtils.getValueFactory();
		LinkedHashMap<String, SnmpValue> varbinds = new LinkedHashMap<String, SnmpValue>();
		varbinds.put(".1.3.6.1.4.1.11.2.14.11.1.7.2.1.4.2404",
				valueFactory.getInt32(3));
		varbinds.put(".1.3.6.1.4.1.11.2.14.11.1.7.2.1.5.2404",
				valueFactory.getInt32(2));
		varbinds.put(".1.3.6.1.4.1.11.2.14.11.1.7.2.1.6.2404",
				valueFactory.getInt32(5));
		varbinds.put(".1.3.6.1.4.1.11.2.14.11.1.7.3.0.2404", valueFactory
				.getOctetString("http://a.b.c.d/cgi/fDetail?index=2404"
						.getBytes()));
		return varbinds;
	}
}
