package org.opennms.netmgt.poller;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangeTimedOutException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.junit.After;
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
public class PollerRoutingTest {

	@Autowired
	ApplicationContext context;
	
	private CamelContext camelContext;
	
	
	@Before
	public void configure() throws Exception {
		camelContext = SpringCamelContext.springCamelContext(context, false);
	    camelContext.start();
	}
	
	@After
	public void closeContext() throws Exception{
	   camelContext.stop();

	}
	
	@Test
	public void testAvailabilityMonitor() throws Exception{
	       try {

		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.AvailabilityMonitor");

		            template.sendBody( "direct:pollAvailabilityMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testCitrixMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.CitrixMonitor");

		            template.sendBody( "direct:pollCitrixMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testDNSMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.DNSMonitor");

		            template.sendBody( "direct:pollDNSMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testDNSResolution() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.DNSResolutionMonitor");

		            template.sendBody( "direct:pollDNSResolutionMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testDominoIIOPMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.DominoIIOPMonitor");

		            template.sendBody( "direct:pollDominoIIOPMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	
	@Test
	public void testFtpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.FtpMonitor");

		            template.sendBody( "direct:pollFtpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}

	@Test
	public void testGpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.GpMonitor");

		            template.sendBody( "direct:pollGpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testHttpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.HttpMonitor");

		            template.sendBody( "direct:pollHttpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testHttpsMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.HttpsMonitor");

		            template.sendBody( "direct:pollHttpsMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testIcmpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.IcmpMonitor");

		            template.sendBody( "direct:pollIcmpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testImapMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.ImapMonitor");

		            template.sendBody( "direct:pollImapMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testJDBCMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.JDBCMonitor");

		            template.sendBody( "direct:pollJDBCMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testJDBCQueryMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.JDBCQueryMonitor");

		            template.sendBody( "direct:pollJDBCQueryMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testJDBCStoredProcedureMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.JDBCStoredProcedureMonitor");

		            template.sendBody( "direct:pollJDBCStoredProcedureMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	
	@Test
	public void testJolokiaBeanMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.JolokiaBeanMonitor");

		            template.sendBody( "direct:pollJolokiaBeanMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testJschSshMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.JschSshMonitor");

		            template.sendBody( "direct:pollJschSshMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testLdapMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.LdapMonitor");

		            template.sendBody( "direct:pollLdapMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testLdapsMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.LdapsMonitor");

		            template.sendBody( "direct:pollLdapsMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	
	@Test
	public void testLoopMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.LoopMonitor");

		            template.sendBody( "direct:pollLoopMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testMemcachedMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.MemcachedMonitor");

		            template.sendBody( "direct:pollMemcachedMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testNrpeMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.NrpeMonitor");

		            template.sendBody( "direct:pollNrpeMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testNtpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.NtpMonitor");

		            template.sendBody( "direct:pollNtpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testPop3Monitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.Pop3Monitor");

		            template.sendBody( "direct:pollPop3Monitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testSmtpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.SmtpMonitor");

		            template.sendBody( "direct:pollSmtpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testSshMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.SshMonitor");

		            template.sendBody( "direct:pollSshMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	
	@Test
	public void testSSLCertMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.SSLCertMonitor");

		            template.sendBody( "direct:pollSSLCertMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testStrafePingMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.StrafePingMonitor");

		            template.sendBody( "direct:pollStrafePingMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	
	@Test
	public void testSystemExecuteMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.SystemExecuteMonitor");

		            template.sendBody( "direct:pollSystemExecuteMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testTcpMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.TcpMonitor");

		            template.sendBody( "direct:pollTcpMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testTrivialTimeMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.TrivialTimeMonitor");

		            template.sendBody( "direct:pollTrivialTimeMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
	
	@Test
	public void testWebMonitor() throws Exception{
	       try {


		            ProducerTemplate template = camelContext.createProducerTemplate();

		            MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
		            monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
		            Map<String,Object> parameters = new HashMap<String, Object>();
		            parameters.put("timeout",new BigDecimal(30.00));
		            parameters.put("retry",new BigDecimal(1));
		            
		            monitoredServiceTask.setParameters(parameters);
		            monitoredServiceTask.setLocation("seda:Location-localhost.Poller.WebMonitor");

		            template.sendBody( "direct:pollWebMonitor", monitoredServiceTask);

		} catch (Exception e) {
			assertEquals(ExchangeTimedOutException.class, e.getCause().getClass());
		}

	}
}
