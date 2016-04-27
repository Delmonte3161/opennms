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

package org.opennms.netmgt.poller;

import java.math.BigDecimal;
import java.util.Map;

/**
 * This class is wrapper that stores MonitoredService and Parameters object.  
 *
 * @author Pradeep Srivatsa <pradeep.s1@cerner.com>
 * 
 */
public class MonitoredServiceTask {

	private MonitoredService m_monitoredService;

	private Map<String, Object> m_parameters;
	
	private String m_location;

	public static final BigDecimal FUDGE_FACTOR = BigDecimal.valueOf(1.5);
	
	public MonitoredServiceTask() {
		super();
	}
	/**
	 * 
	 * @param m_monitoredService
	 * @param m_parameters
	 */
	public MonitoredServiceTask(MonitoredService m_monitoredService,
			Map<String, Object> m_parameters) {
		this.m_monitoredService = m_monitoredService;
		this.m_parameters = m_parameters;
	}
	/**
	 * 
	 * @param m_monitoredService
	 * @param m_parameters
	 * @param location
	 */
	public MonitoredServiceTask(MonitoredService m_monitoredService,
			Map<String, Object> m_parameters,String location) {
		this.m_monitoredService = m_monitoredService;
		this.m_parameters = m_parameters;
		this.m_location = location;
	}

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
	
	/**
	 * 
	 * @param location
	 */
	public void setLocation(String location){
		m_location = location;
	}
	
	/**
	 * 
	 * @return location
	 */
	public String getLocation(){
		 return m_location == null || "".equals(m_location) ? "localhost" : m_location;
	}
	

    /**
     * <P>
     * Returns the total task timeout in milliseconds for all IP ranges.
     * </P>
     */
    public int calculateTaskTimeout() {
        BigDecimal taskTimeOut = BigDecimal.ZERO;
        BigDecimal timeout =  m_parameters.get("timeout") == null ? BigDecimal.valueOf(1) : (BigDecimal)m_parameters.get("timeout");
        BigDecimal retry = m_parameters.get("retry") == null ? BigDecimal.valueOf(1) : (BigDecimal)m_parameters.get("retry");
        taskTimeOut = FUDGE_FACTOR.multiply(timeout).multiply(retry);
        // If the timeout is greater than Integer.MAX_VALUE, just return Integer.MAX_VALUE
        return taskTimeOut.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0 ? Integer.MAX_VALUE : taskTimeOut.intValue();
    }
}
