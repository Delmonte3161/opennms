/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.config.southbound;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.core.xml.ValidateUsing;
import org.opennms.netmgt.config.utils.ConfigUtils;

/**
 * @author tf016851
 *
 */
@XmlRootElement(name = "south-element")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("southbound-configuration.xsd")
public class SouthElement implements Serializable {
    
    private static final long serialVersionUID = 2L;
    
    public static final String DEFAULT_USERID = "opennms";
    public static final String DEFAULT_PASSWORD = "opennms";
    public static final String DEFAULT_SOUTH_CLIENT_API = "org.opennms.netmgt.southd";
    public static final String DEFAULT_SOUTH_MESSAGE_PARSER = "org.opennms.netmgt.southd";

    @XmlAttribute(name = "host", required = true)
    private String m_host;

    @XmlAttribute(name = "port")
    private Integer m_port;

    @XmlAttribute(name = "userid")
    private String m_userid;

    @XmlAttribute(name = "password")
    private String m_password;

    @XmlAttribute(name = "southbound-api")
    private String m_southboundApi;

    @XmlAttribute(name = "southbound-message-parser")
    private String m_southboundMessageParser;

    @XmlAttribute(name = "reconnect-delay")
    private Long m_reconnectDelay;

    public String getHost() {
        return m_host;
    }

    public void setHost(final String host) {
        m_host = ConfigUtils.assertNotEmpty(host, "host");
    }

    public Integer getPort() {
        return m_port != null ? m_port : 443;
    }

    public void setPort(final Integer port) {
        m_port = port;
    }

    public String getUserid() {
        return m_userid != null ? m_userid : DEFAULT_USERID;
    }

    public void setUserid(final String userid) {
        m_userid = ConfigUtils.normalizeAndTrimString(userid);
    }

    public String getPassword() {
        return m_password != null ? m_password : DEFAULT_PASSWORD;
    }

    public void setPassword(final String password) {
        m_password = ConfigUtils.normalizeString(password);
    }

    public String getSouthboundApi() {
        return m_southboundApi != null ? m_southboundApi : DEFAULT_SOUTH_CLIENT_API;
    }

    public void setSouthboundApi(final String southboundApi) {
        this.m_southboundApi = ConfigUtils.normalizeString(southboundApi);
    }

    public String getSouthboundMessageParser() {
        return m_southboundMessageParser != null ? m_southboundMessageParser : DEFAULT_SOUTH_MESSAGE_PARSER;
    }

    public void setSouthboundMessageParser(final String southboundMessageParser) {
        m_southboundMessageParser = ConfigUtils.normalizeString(southboundMessageParser);
    }

    public Long getReconnectDelay() {
        return m_reconnectDelay != null ? m_reconnectDelay : 30000L;
    }

    public void setReconnectDelay(final Long reconnectDelay) {
        m_reconnectDelay = reconnectDelay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_host, 
                            m_port, 
                            m_userid, 
                            m_password, 
                            m_southboundApi, 
                            m_southboundMessageParser, 
                            m_reconnectDelay);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }

        if (obj instanceof SouthElement) {
            final SouthElement that = (SouthElement)obj;
            return Objects.equals(this.m_host, that.m_host)
                    && Objects.equals(this.m_port, that.m_port)
                    && Objects.equals(this.m_userid, that.m_userid)
                    && Objects.equals(this.m_password, that.m_password)
                    && Objects.equals(this.m_southboundApi, that.m_southboundApi)
                    && Objects.equals(this.m_southboundMessageParser, that.m_southboundMessageParser)
                    && Objects.equals(this.m_reconnectDelay, that.m_reconnectDelay);
        }
        return false;
    }

}
