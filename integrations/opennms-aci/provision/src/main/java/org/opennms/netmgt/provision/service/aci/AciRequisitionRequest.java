/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.netmgt.provision.persist.RequisitionRequest;
import org.opennms.netmgt.provision.persist.requisition.Requisition;

/*
 * @author mp050407
 */

@XmlRootElement(name = "aci-requisition-request")
@XmlAccessorType(XmlAccessType.NONE)
public class AciRequisitionRequest implements RequisitionRequest {

	private static final List<String> DEFAULT_SERVICES = Arrays.asList("ICMP");

	@XmlAttribute(name = "hostname")
	private String hostname = null;

	@XmlAttribute(name = "username")
	private String username = null;

	@XmlAttribute(name = "password")
	private String password = null;

	@XmlAttribute(name = "location")
	private String location = null;

	// unique cluster name
	@XmlAttribute(name = "foreign-source")
	private String foreignSource = null;

	@XmlAttribute(name = "apic-url")
	private String apicUrl = null;

	@XmlElement(name = "service")
	private List<String> services;

	@XmlElement(name = "existing-requisition")
	@XmlJavaTypeAdapter(RequisitionXmlAdapter.class)
	private Requisition existingRequisition;

	public AciRequisitionRequest(Map<String, String> parameters) {
		setHostname(parameters.get("hostname"));
		setUsername(parameters.get("username"));
		setPassword(parameters.get("password"));
		setLocation(parameters.get("location"));
		setApicUrl(parameters.get("apic-url"));

	}

	private void setApicUrl(String apicUrl) {
		this.apicUrl = apicUrl;
	}

	public AciRequisitionRequest() {
		// TODO Auto-generated constructor stub
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;

	}

	public void setForeignSource(String foreignSource) {
		this.foreignSource = foreignSource;

	}

	public String getForeignSource() {
		return foreignSource;
	}

	public String getApicUrl() {
		return apicUrl;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setExistingRequisition(Requisition existingRequisition) {
		this.existingRequisition = existingRequisition;
	}

	public List<String> getServices() {
		return services != null ? services : DEFAULT_SERVICES;
	}

	@Override
	public int hashCode() {
		return Objects.hash(hostname, username, password, location, foreignSource, existingRequisition);
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof AciRequisitionRequest)) {
			return false;
		}
		AciRequisitionRequest castOther = (AciRequisitionRequest) other;
		return Objects.equals(hostname, castOther.hostname) && Objects.equals(username, castOther.username)
				&& Objects.equals(password, castOther.password) && Objects.equals(location, castOther.location)
				&& Objects.equals(foreignSource, castOther.foreignSource)
				&& Objects.equals(existingRequisition, castOther.existingRequisition);
	}

}
