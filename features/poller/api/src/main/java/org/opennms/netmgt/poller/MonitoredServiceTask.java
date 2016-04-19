package org.opennms.netmgt.poller;

import java.util.Map;

/**
 * 
 * @author ps044221
 * This class is wrapper that stores MonitoredService and Parameters object.  
 *
 */
public class MonitoredServiceTask {
	
	private MonitoredService m_monitoredService;
	
	private Map<String, Object> m_parameters;

	/**
	 * @return the m_monitoredService
	 */
	public MonitoredService getMonitoredService() {
		return m_monitoredService;
	}

	/**
	 * @param m_monitoredService the m_monitoredService to set
	 */
	public void setMonitoredService(MonitoredService m_monitoredService) {
		this.m_monitoredService = m_monitoredService;
	}

	/**
	 * @return the parameters
	 */
	public Map<String, Object> getParameters() {
		return m_parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(Map<String, Object> parameters) {
		this.m_parameters = parameters;
	}

}
