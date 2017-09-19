package org.opennms.smoketest.utils;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * This is a utility for Health check after mef deployment
 * @author ps044221
 *
 */
public class HealthCheckUtil {
	
	
	  private SearchResult getSearchResult(InetSocketAddress esTransportAddr, String key, String value){
		  
		  JestClient client = null;
		  JestClientFactory factory = new JestClientFactory();
			factory.setHttpClientConfig(new HttpClientConfig.Builder(String
					.format("http://%s:%d", esTransportAddr.getHostString(),
							esTransportAddr.getPort())).multiThreaded(true)
					.build());
			client = factory.getObject();
			SearchResult response = null;

			try {
				 response = client.execute(new Search.Builder(
						new SearchSourceBuilder().query(
								QueryBuilders.matchQuery(key,value))
								.toString()).addIndex("opennms*").build());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				if (client != null) {
					client.shutdownClient();
				}
			}
			
			return response;
	  }
	
	  
	  public Integer pollForEvents(InetSocketAddress esTransportAddr, String parameter, String message) {
			JestClient client = null;
			System.out.println("Parameter:"+parameter);
			System.out.println("Message:"+message);
			try {
				SearchResult response = getSearchResult(esTransportAddr,parameter,message);

				System.out.println("Response:"+response);
				System.out.println("Count:"+response.getTotal());
	            final JsonArray rawHits = response.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");  
				System.out.println("Search response size:"+rawHits.size());
				
				Map<String, String> retMap = new HashMap<String, String>();
				int result = 0;
				System.out.println("Iterating over map");
				 for(int i =0;rawHits.size()>i && result == 0;i++){
		              
		            	final JsonObject hitData = rawHits.get(i).getAsJsonObject();
			              Set<Entry<String,JsonElement>> jsonset = hitData.entrySet();
			              
			              for (Entry<String,JsonElement> s : jsonset) {
			            	 
			            	  if(s.getKey().toString().equalsIgnoreCase("_source")){
			                	  JsonElement js = (JsonElement)s.getValue();
			                	  retMap = new Gson().fromJson(js.toString(), new TypeToken<HashMap<String, String>>() {}.getType());
			                	  
			                      for (Map.Entry<String, String> objects : retMap.entrySet()){
			                    	  if(objects.getKey().equalsIgnoreCase(parameter) && objects.getValue().equalsIgnoreCase(message)){
			                    		 System.out.println(objects.getValue());
			                    		 System.out.println("Messages matched");
			                    		 result = 1;
			                    		 break;
			                    	  }
			                    	  else if(objects.getKey().equalsIgnoreCase(parameter)){
			                    		  System.out.println("Messages did not matched");
			                    		  System.out.println(objects.getValue());
			                    	  }
			                      }
			            	  }
			            	  
			            	  if(result ==1){
			            		  break;
			            	  }
			            }
		            }           
				
				return result;

			} catch (Throwable e) {
				e.printStackTrace();
				return 0;
			} finally {
				if (client != null) {
					client.shutdownClient();
				}
			}
		}
	    
	    
	/**
	 * @param esTransportAddr
	 * @param startOfTest
	 * @return
	 */
	public  Integer pollForTrapEvents(InetSocketAddress esTransportAddr,Date startOfTest) {
			
			try {
				String key = "eventuei";
				String value = "uei.opennms.org/generic/traps/SNMP_Warm_Start";
				
				SearchResult response = getSearchResult(esTransportAddr,key,value);
	            final JsonArray rawHits = response.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");  
				
				Map<String, String> retMap = new HashMap<String, String>();
				
				int result = 0;
	            for(int i=0;i<rawHits.size() && result == 0;i++){
		              
	            	final JsonObject hitData = rawHits.get(i).getAsJsonObject();
		              Set<Entry<String,JsonElement>> jsonset = hitData.entrySet();
		              
		              for (Entry<String,JsonElement> s : jsonset) {
		            	 
		            	  if(s.getKey().toString().equalsIgnoreCase("_source")){
		                	  JsonElement js = (JsonElement)s.getValue();
		                	  retMap = new Gson().fromJson(js.toString(), new TypeToken<HashMap<String, String>>() {}.getType());
		                	  String uei = retMap.get(key);
		                	  if(uei.equalsIgnoreCase(value)){
				                      for (Map.Entry<String, String> objects : retMap.entrySet()){
				                    	  if(objects.getKey().equalsIgnoreCase("p_timestamp") && tokenizeRfcDate(objects.getValue()).after(startOfTest)){
				                    		  System.out.println("Start of Test:"+startOfTest);
				                    		  System.out.println("p_timestam:"+objects.getValue());
				                    		 result = 1;
				                    		 break;
				                    	  }
				                      }
		                	  }

		            	  }
		            	  
	                	  if(result == 1){
	                		  break;
	                	  }
		            }
	            }           
				
				System.out.println("Count:"+response.getTotal());
				
				return result;

			} catch (Throwable e) {
				//LOG.warn(e.getMessage(), e);
				e.printStackTrace();
				return 0;
			} 
		}
	    
	    
	    /**
	     * @param trapAddr
	     * 
	     * sends traps to destination address
	     */
	    public void sendTrap(final InetSocketAddress trapAddr) {
	        //LOG.info("Sending trap");
	    	System.out.println("Sending trap");
	        try {
	            SnmpTrapBuilder pdu = SnmpUtils.getV2TrapBuilder();
	            pdu.addVarBind(SnmpObjId.get(".1.3.6.1.2.1.1.3.0"), SnmpUtils.getValueFactory().getTimeTicks(0));
	            // warmStart
	            pdu.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.1.0"), SnmpUtils.getValueFactory().getObjectId(SnmpObjId.get(".1.3.6.1.6.3.1.1.5.2")));
	            pdu.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.3.0"), SnmpUtils.getValueFactory().getObjectId(SnmpObjId.get(".1.3.6.1.4.1.5813")));
	            pdu.send(InetAddressUtils.str(trapAddr.getAddress()), trapAddr.getPort(), "public");
	        } catch (Throwable e) {
	           // LOG.error(e.getMessage(), e);
	        	e.printStackTrace();
	        }
	        //LOG.info("Trap has been sent");
	        System.out.println("Trap has been sent");
	    }
	    
	    /**
	     * @param heartBeatResult
	     * @param syslogResult
	     * @param trapResult
	     * 
	     * prints results after the health check is completed
	     */
	    public void printResult(boolean heartBeatResult,boolean syslogResult,boolean trapResult){
	    	String star = "*";
	    	String hash = "#";
	    	for(int i=0;i<5;i++){
	    		star = star + star; 
	    		hash = hash + hash;
	    	}
	    	System.out.println(hash);
	    	System.out.println();
	    	System.out.println("    Health Check Results          ");
	    	System.out.println();
	    	System.out.println(star);
	    	System.out.print("HeatBeat Test Result:");
	    	System.out.print((heartBeatResult == true)? "Passed" : "Failed" ); 
	    	System.out.println();
	    	System.out.println(star);
	    	System.out.print("Syslog Test Result:");
	    	System.out.println((syslogResult == true)? "Passed" : "Failed");
	    	System.out.println(star);
	    	System.out.print("Trap Test Result:");
	    	System.out.println((trapResult == true)? "Passed" : "Failed");
	    	System.out.println(hash);
	    }
	    
	    
		private Date tokenizeRfcDate(String dateString)  {
			DateFormat eventDate=new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			try {
				return eventDate.parse(dateString);
			} catch (java.text.ParseException e) {
				System.out.println("Failed to convert string to date");
				return null;
			}
	}
		
	    /**
	     * @param minionSshAddr
	     * @param syslog
	     */
	    public void sendSyslog(String minionSshAddr,String syslog) {
	        try {
	            //builder.send(m_trapSink.getHostAddress(), m_trapPort.intValue(), m_trapCommunity);
	            byte[] bytes = syslog.getBytes();
	           
	            //DatagramPacket pkt = new DatagramPacket(bytes, bytes.length, InetAddrUtils.addr("127.0.0.1"), 514);
	            DatagramPacket pkt = new DatagramPacket(bytes, bytes.length, new InetSocketAddress(minionSshAddr, 514));
	            DatagramSocket datagramSocket = new DatagramSocket();
	            datagramSocket.send(pkt);
	            datagramSocket.close();
	        } catch (Exception e) {
	            throw new IllegalStateException("Caught Exception sending syslog.", e);
	        }
	    }

}
