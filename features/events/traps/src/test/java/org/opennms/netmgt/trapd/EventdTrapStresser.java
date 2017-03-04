/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opennms.netmgt.snmp.InetAddrUtils;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpV1TrapBuilder;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.SnmpValueFactory;
import org.opennms.netmgt.snmp.snmp4j.Snmp4JStrategy;
import org.snmp4j.CommandResponder;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

/**
 * @author MS043660
 */
public class EventdTrapStresser {

	private static final String DEFAULT_IPADDRESS = "127.0.0.1";
	private static InetAddress m_agentAddress;
	private static InetAddress m_trapSink;
	private static Integer m_trapPort = Integer.valueOf(1162);
	private static String m_trapCommunity = "public";
	private static Double m_trapRate = Double.valueOf(100); // seconds
	private static Integer m_trapCount = Integer.valueOf(500);
	private static Integer m_batchDelay = Integer.valueOf(1); // seconds
	private static Integer m_batchSize = m_trapCount;
	private static int m_batchCount = 1;
	private static String m_postgresAddress;
	private static int totalTrap;

	private static SnmpTrapBuilder builderV1;
	private static SnmpTrapBuilder builderV2;

	private static Long totalTimeNeededToSend;
	private static Connection connection;
	private static long totalElapsedTime = 0L;

	private static long totalSeconds;

	private static List<Integer> uniqueList = new ArrayList<Integer>();
	private static Snmp session;

	private static Thread m_thread;
	
	private static int m_trapSent;

	public static void main(String[] args) throws Exception {

		m_trapRate = Double.valueOf(args[0]); // seconds
		m_trapCount = Integer.valueOf(args[1]);
		m_agentAddress = InetAddrUtils.addr(args[2]);
		m_batchCount = Integer.valueOf(args[3]);
		if (args.length == 5)
			m_postgresAddress = args[4];

		setIpAddresses();
		if (m_postgresAddress != null)
			dataBaseConnect();

		m_batchSize = m_trapCount;
		totalTrap = m_trapCount * m_batchCount;
		System.out.println();
		System.out.println("m_trapRate : " + m_trapRate);
		System.out.println("m_trapCount : " + m_trapCount);
		System.out.println("m_agentAddress : " + m_agentAddress);
		System.out.println("m_batchCount : " + m_batchCount);
		//
		// System.out.println("Commencing the Eventd Stress Test...");

		builderV1 = createBuilderV1();
		builderV2 = createBuilderV2();
		  LogFactory.setLogFactory(new Log4jLogFactory());
		  executeStressTest();

		if (m_postgresAddress != null) {
			eventsTableCount();
			getTimeStamp();
			connection.close();
		}
		System.out.println("Total Number of Traps recived at SNMP send "+Snmp4JStrategy.setOfInt.size());
	

	}


	private static void getTimeStamp() {
		try {
			Timestamp startTime = null;
			Timestamp endTime = null;
			Statement stmt = null;
			if (connection != null) {
				int i = 0;
				stmt = connection.createStatement();
				String sql = "(select eventcreatetime at time zone 'UTC'  from events where eventsource = 'trapd' order by eventcreatetime asc limit 1) union all (select eventcreatetime at time zone 'UTC'  from events where eventsource = 'trapd' order by eventcreatetime desc limit 1);";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					if (i == 0) {
						startTime = rs.getTimestamp(1);
						i++;
					} else {
						endTime = rs.getTimestamp(1);
					}
				}
				long startTimeInMillis = startTime.getTime();
				long endTimeMills = endTime.getTime();
				long totalMillis = endTimeMills - startTimeInMillis;
				Long totalSecondsDatabase = totalMillis / 1000L;
				System.out
						.println("Total Elapsed time (secs) to write in database : "
								+ (totalSecondsDatabase));
				stmt.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Counting events failed!!");
		}

	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
	}

	private static void eventsTableCount() {
		try {
			Thread.sleep(1000);
			int numberOfEvents = 0;
			Statement stmt = null;
			if (connection != null) {
				stmt = connection.createStatement();
				String sql = "select count(*) from events where eventsource = 'trapd'";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					numberOfEvents = rs.getInt(1);

				}
				if (numberOfEvents != 0
						&& numberOfEvents < totalTrap
						&& Collections.frequency(uniqueList, numberOfEvents) < 10) {
					uniqueList.add(numberOfEvents);
					eventsTableCount();
				} else {
					System.out
							.println("Total number of traps events in opennms database :"
									+ numberOfEvents);

					System.out.println("Total number of traps loss  :"
							+ (totalTrap - numberOfEvents));
				}

				stmt.close();
			}
		} catch (Exception e) {
			System.out.println("Counting events failed!!");
		}

	}

	private static void executeStressTest() {
		try {
			session = createSnmpSession();
			stressEventd();
			closeQuietly();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private synchronized static void closeQuietly() {
		if (session == null) {
			return;
		}

		try {
			session.close();
		} catch (IOException e) {
		}
	}

	private static void setIpAddresses() {
		try {
			if (!m_agentAddress.getHostAddress().equalsIgnoreCase(
					DEFAULT_IPADDRESS)) {
				m_trapSink = m_agentAddress;
			} else {
				m_trapSink = InetAddress.getByName(DEFAULT_IPADDRESS);
			}
		} catch (UnknownHostException e1) {
			System.exit(1);
		}
	}

	public static void stressEventd() throws Exception {

		if (m_batchCount < 1) {
			throw new IllegalArgumentException(
					"Batch count of < 1 is not allowed.");
		} else if (m_batchCount > m_trapCount) {
			throw new IllegalArgumentException(
					"Batch count is > than trap count.");
		}

		totalTimeNeededToSend = (long) ((m_trapCount.doubleValue() / m_trapRate
				.doubleValue()) * m_batchCount);
		System.out.println("Estimated time to send Complete: "
				+ (m_trapCount.doubleValue() / m_trapRate.doubleValue())
				* m_batchCount + " seconds");
		System.out.println("Sending " + m_trapCount + " traps in "
				+ m_batchCount + " batches with a batch interval of "
				+ m_batchDelay.toString() + " seconds...");

		long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
		int trapsSent = sendTraps(builderV1, builderV2, startTimeInMillis);

		systemReport(0L, trapsSent, totalElapsedTime);
	}

	private static void systemReport(long beginMillis, int trapsSent,
			long stopTimeInMillis) {
		m_trapSent=trapsSent;
		System.out.println("  Traps sent: " + trapsSent);
		long totalMillis = stopTimeInMillis - beginMillis;
		totalSeconds = totalMillis / 1000L;
		System.out.println("Total Elapsed time (secs): " + totalSeconds);

		Long finalTime = (totalSeconds - totalTimeNeededToSend);
		if (finalTime.intValue() < 0) {
			System.out.println("Time left over than estimated time(secs): "
					+ Math.abs(finalTime));
		} else {
			System.out
					.println("Extra Time taken to finish(secs): " + finalTime);
		}

		
		System.out.println();
	}

	private static void dataBaseConnect() {
		connection = null;
		Statement stmt = null;
		try {
			connection = DriverManager
					.getConnection("jdbc:postgresql://" + m_postgresAddress
							+ ":5432/opennms", "opennms", "opennms");

			if (connection != null) {
				System.out.println("Connected to database now!");
				stmt = connection.createStatement();
				// eventsTableCount();
				String sql = "DELETE FROM events";
				stmt.executeUpdate(sql);
				System.out.println("Deleted rows in Events Table successfully");
				stmt.close();
			} else {
				System.out.println("Failed to make connection!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Connection Failed");
			return;
		}
	}

	private synchronized static int sendTraps(SnmpTrapBuilder builderV1,
			SnmpTrapBuilder builderV2, long beginMillis)
			throws IllegalStateException, InterruptedException, SQLException,
			IOException {

		int totalTrapsSent = 0;
		Double currentRate = 0.0;
		Long batchTrapsSent = 0L;
		Long elapsedMillis = 0L;
		Long batchElapsedMillis = 0L;
		Long batchBegin = 0L;
		int remainingTraps = 0;
		totalElapsedTime = 0L;

		for (int i = 1; i <= m_batchCount; i++) {

			batchBegin = Calendar.getInstance().getTimeInMillis();
			batchTrapsSent = 0L;
			System.out.println("Sending batch " + i + " of "
					+ Integer.valueOf(m_batchCount) + " batches of "
					+ m_batchSize.intValue() + " traps at the rate of "
					+ m_trapRate.toString() + " traps/sec...");
			System.out.println("Estimated time to send: "
					+ m_batchSize.doubleValue() / m_trapRate.doubleValue()
					+ " seconds");

			while (batchTrapsSent.intValue() < m_batchSize.intValue()) {

				if (currentRate <= m_trapRate || batchElapsedMillis == 0) {
					batchTrapsSent += sendTrap(getTrapbuilder(batchTrapsSent));
				} else {
					Thread.sleep(1);
				}

				batchElapsedMillis = Calendar.getInstance().getTimeInMillis()
						- batchBegin;
				currentRate = batchTrapsSent.doubleValue()
						/ batchElapsedMillis.doubleValue() * 1000.0;

				if (batchElapsedMillis % 1000 == 0) {
					System.out.print(".");
				}

			}
			System.out.println();
			totalTrapsSent += batchTrapsSent;
			System.out.println("   Actual time to send: "
					+ (batchElapsedMillis / 1000.0 + " seconds"));

			totalElapsedTime += batchElapsedMillis;
			System.out.println("Elapsed Time (secs): "
					+ (totalElapsedTime / 1000L));

			System.out.println("         Traps sent: "
					+ Integer.valueOf(totalTrapsSent).toString());
			System.out.println();
		}

		remainingTraps = (m_trapCount * m_batchCount) - totalTrapsSent;
		if (remainingTraps > 0) {
			System.out.println("Sending batch remainder of " + remainingTraps
					+ " traps...");
			batchBegin = Calendar.getInstance().getTimeInMillis();

			while (batchTrapsSent.intValue() < remainingTraps) {

				if (currentRate <= m_trapRate || elapsedMillis == 0) {
					batchTrapsSent += sendTrap(getTrapbuilder(batchTrapsSent));
				} else {
					Thread.sleep(1);
				}

				elapsedMillis = Calendar.getInstance().getTimeInMillis()
						- batchBegin;
				currentRate = batchTrapsSent.doubleValue()
						/ elapsedMillis.doubleValue() * 1000.0;
			}

			totalTrapsSent += batchTrapsSent;
			System.out.println("Elapsed Time (secs): "
					+ ((System.currentTimeMillis() - beginMillis) / 1000L));
			System.out.println("         Traps sent: "
					+ Integer.valueOf(totalTrapsSent).toString());
		}
		return totalTrapsSent;
	}

	private static SnmpTrapBuilder getTrapbuilder(Long batchTrapsSent) {
		if (batchTrapsSent % 2 == 0) {
			return builderV2;
		}
		return builderV1;

	}

	public synchronized static  Snmp createSnmpSession() throws IOException {
		
		  ThreadPool threadPool = ThreadPool.create("DispatcherPool", 10);
		    MessageDispatcher mtDispatcher = new MultiThreadedMessageDispatcher(threadPool, 
		            new MessageDispatcherImpl());

		    mtDispatcher.addMessageProcessingModel(new MPv1());
		    mtDispatcher.addMessageProcessingModel(new MPv2c());

		DefaultUdpTransportMapping defaultTransport = new DefaultUdpTransportMapping();
		defaultTransport.setReceiveBufferSize(500000000);

		Snmp session = new Snmp(mtDispatcher,defaultTransport);
		defaultTransport.listen();
		return session;

	}

	private synchronized static int sendTrap(final SnmpTrapBuilder builder) {
		int trapsSent = 0;
		try {
				builder.send(m_trapSink.getHostAddress(), m_trapPort,
						m_trapCommunity, session);

				trapsSent++;
			return trapsSent;
		} catch (Exception e) {
			throw new IllegalStateException("Caught Exception sending trap.", e);
		}
	}

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

	public static SnmpTrapBuilder createBuilderV1() throws Exception {
		// Comes as Normal
		SnmpV1TrapBuilder pdu = SnmpUtils.getV1TrapBuilder();
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
		return pdu;
	}

	public static SnmpTrapBuilder createBuilderV2() {
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
		return pdu;
	}

}
