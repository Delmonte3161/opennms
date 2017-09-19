package org.opennms.smoketest.minion;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.hibernate.MinionDaoHibernate;
import org.opennms.netmgt.model.minion.OnmsMinion;
import org.opennms.smoketest.utils.DaoUtils;
import org.opennms.test.system.api.NewTestEnvironment.ContainerAlias;
import org.opennms.test.system.api.TestEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ps044221
 * Load all syslog patterns into a map -> Send to ES -> verify
 *
 */
public class SyslogAllPatternTest extends AbstractSyslogTestCase {

	private static final Logger LOG = LoggerFactory
			.getLogger(SyslogAllPatternTest.class);

	private Map<String,Map<String,String>> messageUEIs = new HashMap<String,Map<String,String>>();

	
	public void readFileAndLoadMessagesAndUei() {
		ClassLoader classLoader = getClass().getClassLoader();

		try {
			File fileName = new File(classLoader.getResource(
					"SyslogAllMessagesUEI.txt").getFile());

			String filename = fileName.getAbsolutePath().replaceAll("%20", " ");
			BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
			String line = "";

			while ((line = bufferedReader.readLine()) != null) {

				if (line.startsWith("Message")) {

					String[] parts = line.split(":", 2);
					String key = parts[1];
					line = bufferedReader.readLine();

					Map<String, String> paramsMap = new HashMap<String, String>();

					for (; !line.isEmpty(); line = bufferedReader.readLine()) {

						parts = line.split(":", 2);
						paramsMap.put(parts[0], parts[1]);

					}
					messageUEIs.put(key, paramsMap);
				}

			}

		}

		catch (Exception e) {
			e.printStackTrace();
			LOG.debug("Exception!");
		}
	}

	@Override
	protected TestEnvironmentBuilder getEnvironmentBuilder() {
		TestEnvironmentBuilder builder = super.getEnvironmentBuilder();
		return builder;
	}

	@Test
	public void testAllPatterns() throws Exception {
		Date startOfTest = new Date();
		ContainerAlias containerAlias = getMinionAlias();

		InetSocketAddress minionSshAddr = testEnvironment.getServiceAddress(containerAlias, 8201);
		InetSocketAddress esRestAddr = testEnvironment.getServiceAddress(ContainerAlias.ELASTICSEARCH_5, 9200);
		InetSocketAddress opennmsSshAddr = testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 8101);
		InetSocketAddress kafkaAddress = testEnvironment.getServiceAddress(ContainerAlias.KAFKA, 9092);
		InetSocketAddress zookeeperAddress = testEnvironment.getServiceAddress(ContainerAlias.KAFKA, 2181);

		// Install the Kafka syslog and trap handlers on the Minion system
		installFeaturesOnMinion(minionSshAddr, kafkaAddress);

		// Install the Kafka and Elasticsearch features on the OpenNMS system
		installFeaturesOnOpenNMS(opennmsSshAddr, kafkaAddress, zookeeperAddress);

        // Wait for the minion to show up
		
        await().atMost(300, SECONDS).pollInterval(60, SECONDS)
            .until(DaoUtils.countMatchingCallable(
                 getDaoFactory().getDao(MinionDaoHibernate.class),
                 new CriteriaBuilder(OnmsMinion.class)
                     .gt("lastUpdated", startOfTest)
                     .eq("location", "MINION")
                     .toCriteria()
                 ),
                 is(1)
             );



		for (int i = 0; i < 10; i++) {
			LOG.info("Slept for " + i + " seconds");
			Thread.sleep(1000);
		}

		LOG.info("Resetting statistics");
		resetRouteStatistics(opennmsSshAddr, minionSshAddr);
		// Warm up the routes
		//sendMessage(containerAlias, sender, 1);
		readFileAndLoadMessagesAndUei();
				
		final int chunk = 100;
		int resendCount = 0;
		boolean failed = false;

		for (Entry<String, Map<String, String>> entry : messageUEIs.entrySet()) {

			String message = entry.getKey();
			Map<String,String> paramsMap = entry.getValue();

			int mapSize = 0;
			Map<String, String> retMap = new HashMap<String, String>();
			
			while (mapSize == 0) {

				resendCount++;
				LOG.info("Sending Packet:"+resendCount);
				sendMessage(containerAlias, null,chunk, message);
				Thread.sleep(30000);

				retMap = pollForElasticsearchEventsUsingJestAndReturnValue(esRestAddr, paramsMap,message);
				mapSize = retMap.size();
				
				if (resendCount > 15) {
					LOG.info("Timed out :( Test failed! ");
					failed = true;
					break;
				}
			}	
			
			// stop the test if message was not received
			if (failed) {
				break;
			}
			
			
      	  LOG.debug("Map Size:"+retMap.size());
			//check if the event object is created as expected.
            for (Map.Entry<String, String> objects : paramsMap.entrySet())
            {
            	String actualValue = objects.getValue();
            	String returnedValue = retMap.get(objects.getKey());

            	
                if(!actualValue.equalsIgnoreCase(returnedValue)){
                	
                	if(actualValue.isEmpty() || actualValue==""){
                		continue;
                	}
                	
                	
                	LOG.debug("Test failed! values are different");
                	
                	LOG.debug("Key:"+objects.getKey());
                	LOG.debug("actualValue:"+actualValue);
                	LOG.debug("returnedValue:"+returnedValue);
                	failed = true;
                	break;
                }
                
            }

			resendCount = 0;
			if (failed) {
				break;
			}
		}
		assertTrue(failed != true);
		LOG.info("Completed Test");
	}

}
