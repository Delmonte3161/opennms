/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.service.aci;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.commons.io.IOExceptionWithCause;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.service.aci.AciRequisitionProvider;
import org.opennms.netmgt.provision.service.aci.AciImportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of <code>java.net.URLConnection</code> for handling
 * URLs specified in the Provisiond configuration requesting an import
 * requisition based on the A records of a zone transfer for a DNS server.
 *
 * @author mp050407
 * 
 */
public class AciRequisitionUrlConnection extends URLConnection{
	private static final Logger logger = LoggerFactory.getLogger(AciRequisitionUrlConnection.class);

    private static final String EXPRESSION_ARG = "expression";
    
    private static final String SERVICES_ARG = "services";
    
    private static final String FID_HASH_SRC_ARG = "foreignidhashsource";
    
    private static final String[] HASH_IP_KEYWORDS = { "ip", "addr" };
    
    private static final String[] HASH_LABEL_KEYWORDS = { "name", "label" };

    private static final String QUERY_ARG_SEPARATOR = "&";

    /** Constant <code>URL_SCHEME="aci://"</code> */
    public static final String URL_SCHEME = "aci://";

    /** Constant <code>PROTOCOL="aci"</code> */
    public static final String PROTOCOL = "aci";

    private static final AciRequisitionProvider s_provider = new AciRequisitionProvider();
    
    @SuppressWarnings("unused")
	private final AciImportRequest m_request;
    @SuppressWarnings("unused")
	private final Map<String, String> m_args = null;

	protected AciRequisitionUrlConnection(URL url) throws MalformedURLException{
		super(url);
		logger.debug("AciRequisitionUrlConnection constrcutor");
		m_request = new AciImportRequest();
	}


	


	@Override
	public void connect() throws IOException {
		logger.debug("AciRequisitionUrlConnection:connect");
		
	}
	
	/**
     * {@inheritDoc}
     *
     * Creates a ByteArrayInputStream implementation of InputStream of the XML marshaled version
     * of the Requisition class.  Calling close on this stream is safe.
     */
    @Override
    public InputStream getInputStream() throws IOException {
    	System.out.println("getInputstream of Acireq url");
    	logger.debug("AciRequisitionUrlConnection:getInputStream");
        try {
            final Requisition r = s_provider.getRequisition(m_request);
            return new ByteArrayInputStream(jaxBMarshal(r).getBytes());
        } catch (Exception e) {
            String message = "Problem getting input stream: "+e;
            logger.warn(message, e);
            throw new IOExceptionWithCause(message,e );
        }
    }


	private String jaxBMarshal(Requisition r) {
		return JaxbUtils.marshal(r);
	}


	
	public AciImportRequest getRequest() {
		logger.debug("AciRequisitionUrlConnection:getRequest");
        return m_request;
    }

}
