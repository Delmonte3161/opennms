package org.opennms.netmgt.poller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangeTimedOutException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.poller.mock.MockMonitoredService;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@RunWith( OpenNMSJUnit4ClassRunner.class )
@ContextConfiguration( locations = { 
		"classpath:/META-INF/opennms/applicationContext-soa.xml",
		"classpath:/META-INF/opennms/applicationContext-pollerd.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/applicationContext-eventUtil.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-testMockEventIpcManager.xml"
		} )

@JUnitConfigurationEnvironment 
@JUnitTemporaryDatabase(tempDbClass=MockDatabase.class,reuseDatabase=false)
public class PollerRoutingTest extends RouteBuilder {

	@Autowired
	ApplicationContext context;
	
	
	@Before
	public void configure() throws Exception {
		 {
             // Add exception handlers
             onException( IOException.class ).handled( true ).logStackTrace( true ).stop();

             from( "direct:pollAvailabilityMonitor" ).setHeader("CamelJmsRequestTimeout", simple("40000",Long.class)).to( "bean:availabilityMonitorCamel" ).split( body() ).recipientList(
                             simple( "seda:Location-${body.location}.Poller.AvailabilityMonitor" ) );

             from( "seda:Location-localhost.Poller.AvailabilityMonitor" ).to( "bean:availabilityMonitor?method=poll" );
             
         }
		
	}
	
	@Test
	public void testPoller() throws Exception{
		CamelContext camelContext = null;
	       try {
			camelContext = SpringCamelContext.springCamelContext(

					   context, false);


		            camelContext.start();

		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(6000.00));
		            parameters.put("retry",new BigDecimal(2));
		            System.out.println((BigDecimal)parameters.get("timeout"));
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.AvailabilityMonitor");

		            template.sendBody( "direct:pollAvailabilityMonitor", monitoredServiceTask);

		} catch (Exception e) {
			//e.printStackTrace();
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}
	       finally {
	            camelContext.stop();

	        }

	}

}
