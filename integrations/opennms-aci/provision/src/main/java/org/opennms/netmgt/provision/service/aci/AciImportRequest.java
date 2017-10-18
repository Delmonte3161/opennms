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
import org.opennms.netmgt.provision.service.aci.RequisitionXmlAdapter;

/**
 * @author mp050407
 */
@XmlRootElement(name = "aci-requisition-request")
@XmlAccessorType(XmlAccessType.NONE)
public class AciImportRequest implements RequisitionRequest {

    public static final List<String> DEFAULT_SERVICES = Arrays.asList("ICMP",
                                                                       "SSH");

    @XmlAttribute(name = "hostname")
    private String hostname = null;

    @XmlAttribute(name = "username")
    private String username = null;

    @XmlAttribute(name = "password")
    private String password = null;

    @XmlAttribute(name = "cluster-name")
    private String clusterName = null;

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

    public AciImportRequest(Map<String, String> parameters) {
        if (parameters.get("hostname") != null)
            setHostname(parameters.get("hostname"));

        if (parameters.get("username") != null)
            setUsername(parameters.get("username"));

        if (parameters.get("password") != null)
            setPassword(parameters.get("password"));

        if (parameters.get("apic-url") != null)
            setApicUrl(parameters.get("apic-url"));

        if (parameters.get("location") != null)
            setLocation(parameters.get("location"));

        setClusterName(parameters.get("cluster-name"));
        
        if (clusterName == null) {
            throw new IllegalArgumentException("cluster-name is required");
        }
        
        //Use the clusterName as the foreign source
        if (!clusterName.equals(foreignSource))
            setForeignSource(clusterName);
        
//        String path = parameters.get("path");
//        if (path == null) {
//            throw new IllegalArgumentException("path is required");
//        }

//        path = path.replaceAll("^/", "");
//        path = path.replaceAll("/$", "");

//        setForeignSource(path); //path part of resource should be the apic cluster name, which is our foriegn-source
//        String[] pathElements = path.split("/");
//
//        if (pathElements.length == 1) {
//            if ("".equals(pathElements[0])) {
//                setForeignSource("vmware-" + getHostname());
//            } else {
//                setForeignSource(pathElements[0]);
//            }
//        } else {
//            throw new IllegalArgumentException("Error processing path element of URL (aci://host[/foreign-source]?keyA=valueA;keyB=valueB;...)");
//        }

    }

    public AciImportRequest() {
        // TODO Auto-generated constructor stub
    }

    public void setApicUrl(String apicUrl) {
        this.apicUrl = apicUrl;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public String getHostname() {
        return hostname;
    }

    public String getClusterName() {
        return clusterName;
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

    /**
     * @return the existingRequisition
     */
    public Requisition getExistingRequisition() {
        return existingRequisition;
    }

    public List<String> getServices() {
        return services != null ? services : DEFAULT_SERVICES;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, username, password, clusterName, apicUrl,
                            foreignSource, existingRequisition);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AciImportRequest)) {
            return false;
        }
        AciImportRequest castOther = (AciImportRequest) other;
        return Objects.equals(hostname, castOther.hostname)
                && Objects.equals(username, castOther.username)
                && Objects.equals(password, castOther.password)
                && Objects.equals(clusterName, castOther.clusterName)
                && Objects.equals(apicUrl, castOther.apicUrl)
                && Objects.equals(foreignSource, castOther.foreignSource)
                && Objects.equals(existingRequisition,
                                  castOther.existingRequisition);
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
