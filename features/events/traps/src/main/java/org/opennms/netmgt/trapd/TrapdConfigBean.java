package org.opennms.netmgt.trapd;

import java.util.List;

import org.opennms.netmgt.config.TrapdConfig;
import org.opennms.netmgt.snmp.SnmpV3User;

public final class TrapdConfigBean implements TrapdConfig {

	private String m_snmpTrapAddress;
	private int m_snmpTrapPort;
	private boolean m_newSuspectOnTrap;
	private List<SnmpV3User> m_snmpV3Users;

	public void setSnmpTrapAddress(String snmpTrapAddress) {
		this.m_snmpTrapAddress = snmpTrapAddress;
	}

	public void setSnmpTrapPort(int snmpTrapPort) {
		this.m_snmpTrapPort = snmpTrapPort;
	}

	public void setNewSuspectOnTrap(boolean newSuspectOnTrap) {
		this.m_newSuspectOnTrap = newSuspectOnTrap;
	}

	public void setSnmpV3Users(List<SnmpV3User> snmpV3Users) {
		this.m_snmpV3Users = snmpV3Users;
	}

	@Override
	public String getSnmpTrapAddress() {
		return m_snmpTrapAddress;
	}

	@Override
	public int getSnmpTrapPort() {
		return m_snmpTrapPort;
	}

	@Override
	public boolean getNewSuspectOnTrap() {
		return m_newSuspectOnTrap;
	}

	@Override
	public List<SnmpV3User> getSnmpV3Users() {
		return m_snmpV3Users;
	}

}
