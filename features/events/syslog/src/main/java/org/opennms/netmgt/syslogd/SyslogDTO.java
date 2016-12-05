/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.syslogd;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.opennms.core.utils.InetAddressUtils;

@XmlRootElement(name = "syslog-dto")
@XmlAccessorType(XmlAccessType.NONE)
public class SyslogDTO {
	
	public String SYSTEM_ID = "systemId";
	public String LOCATION = "location";
	public String SOURCE_ADDRESS = "sourceAddress";
	public String SOURCE_PORT = "sourcePort";
	
	private ByteBuffer m_body;
	
	public SyslogDTO(InetAddress sourceAddress,String sourcePort, ByteBuffer byteBuffer, String systemId, String location){
		this.SOURCE_ADDRESS = InetAddressUtils.str(sourceAddress);
		this.SOURCE_PORT = sourcePort;
		this.SYSTEM_ID = systemId;
		this.LOCATION = location;
		this.m_body = byteBuffer;
	}

	@XmlAttribute
	public String getSystemId() {
		return SYSTEM_ID;
	}

	@XmlAttribute
	public String getLocation() {
		return LOCATION;
	}

	@XmlAttribute
	public String getSourceAddress() {
		return SOURCE_ADDRESS;
	}

	@XmlAttribute
	public String getSourcePort() {
		return SOURCE_PORT;
	}

	@XmlAttribute
	public byte[] getBody() {
		m_body.rewind();
        byte[] retval = new byte[m_body.remaining()];
        m_body.get(retval);
        m_body.rewind();
        return retval;
	}

	public void setBody(ByteBuffer m_body) {
		this.m_body = m_body;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("systemId",getSystemId())
				.append("location", getLocation())
				.append("sourceAddress", getSourceAddress())
				.append("sourcePort", getSourcePort())
				.append("body", getBody()).toString();
	}

}
