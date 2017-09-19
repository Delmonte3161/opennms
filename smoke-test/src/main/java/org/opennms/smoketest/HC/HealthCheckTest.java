package org.opennms.smoketest.HC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.api.MinionDao;
import org.opennms.netmgt.dao.hibernate.MinionDaoHibernate;
import org.opennms.netmgt.model.minion.OnmsMinion;
import org.opennms.smoketest.utils.DaoUtils;
import org.opennms.smoketest.utils.HealthCheckUtil;
import org.opennms.smoketest.utils.HibernateDaoFactory;

/**
 * @author ps044221
 * This class is for Health check after MEF deployment.
 * The program will check Heartbeat,syslogs and traps. Finally it will print a report.
 * The utility needs 4 parameters to run namely minion address, ElasticSearch address, Postgres address and minion name (in controller file)
 */

public class HealthCheckTest {
	
	private static String minionSshAddr;
	private static String esRestAddr;
	private static String postgressAddress;
	private static String minionName;
	private static HealthCheckUtil healthCheckUtil = new HealthCheckUtil();
	
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
    	HealthCheckTest healthCheckTest = new HealthCheckTest();
    	
    	minionSshAddr = args[0];
    	esRestAddr = args[1];
    	postgressAddress = args[2];
    	minionName = args[3];
    	
    	boolean heartBeatResult = false,syslogResult = false,trapResult = false;
    	
    	heartBeatResult = healthCheckTest.testHeartBeat();
    	System.out.println("Starint Syslog Test");
    	syslogResult = healthCheckTest.testSyslog();
    	System.out.println("Starint Trap Test");
    	trapResult = healthCheckTest.testTrap();
    	
    	
    	healthCheckUtil.printResult(heartBeatResult,syslogResult,trapResult);
    	System.exit(0);
    	
    }
    
    /**
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private boolean testHeartBeat() throws InterruptedException, ExecutionException{
		HibernateDaoFactory daoFactory = new HibernateDaoFactory(new InetSocketAddress(postgressAddress, 5432),true);
		MinionDao minionDao = daoFactory.getDao(MinionDaoHibernate.class);
		
		System.out.println("Starting the HeartBeat HC for "+minionName);
		Date startOfTest = new Date();
		
		int count = 0;
		boolean result = true;
		
		for(int i=0;i<5 && count == 0;i++){
        
	        ExecutorService executorService = Executors.newFixedThreadPool(1);
	        count = executorService.submit(DaoUtils.countMatchingCallable(
	        		minionDao,
	             new CriteriaBuilder(OnmsMinion.class)
	                 .gt("lastUpdated", startOfTest)
	                 .eq("location", minionName)
	                 .toCriteria()
	             )).get();
	        Thread.sleep(60000);
		}
        
        if (count == 0){
        	System.out.println("HeartBeat Test failed");
        	result = false;
        }
        
        System.out.println("Completed Heartbeat Test");
        return result;
    }
    
    /**
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean testSyslog() throws IOException, InterruptedException{
        Date myDate = new Date();
        String date = new SimpleDateFormat("yyyy-MM-dd").format(myDate);
        String syslog = "<34> "+date+" 10.181.230.67 foo10000: load test 10000 on ";
        
        String randomString = "" + myDate.getHours() + myDate.getMinutes() + myDate.getSeconds();
        syslog = syslog + randomString;
        
        System.out.println("Sending the syslog message:"+syslog);
        
        int resendCount = 0;
        InetSocketAddress elasticsearchAddress = new InetSocketAddress(esRestAddr, 9200);
        
        boolean result = true;
        while(healthCheckUtil.pollForEvents(elasticsearchAddress,"p_rawmessage",syslog) == 0) {
        	
           resendCount++;
           System.out.println("Resending Packets:"+resendCount);
           healthCheckUtil.sendSyslog(minionSshAddr,syslog);
     	   Thread.sleep(60000);
      	   if(resendCount>30){
      		     System.out.println("Timed out :( Test failed! ");
      		     result = false;
        		 break;
      	   }
        }
        
        return result;
    }

	/**
	 * @return
	 * @throws InterruptedException
	 */
	private boolean testTrap() throws InterruptedException{
		
		final InetSocketAddress trapAddr = new InetSocketAddress(minionSshAddr, 162);

		int resendCount = 0;
        InetSocketAddress elasticsearchAddress = new InetSocketAddress(esRestAddr, 9200);
        
        boolean result = true;
        Date startOfTest = Calendar.getInstance().getTime(); 
        while(healthCheckUtil.pollForTrapEvents(elasticsearchAddress,startOfTest) == 0) {
        	
            resendCount++;
            System.out.println("Resending traps:"+resendCount);
            healthCheckUtil.sendTrap(trapAddr);
      	    Thread.sleep(60000);
       	    if(resendCount>30){
       		     System.out.println("Timed out :( Test failed! ");
       		     result = false;
         		 break;
       	    }
         }

        return result;
    }
    
}
