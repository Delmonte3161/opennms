package org.opennms.netmgt.trapd;

import java.util.List;

import org.opennms.netmgt.config.TrapdConfig;
import org.opennms.netmgt.snmp.SnmpV3User;

public final class TrapdConfigBean implements TrapdConfig {

	private String m_snmpTrapAddress;
	private int m_snmpTrapPort;
	private boolean m_newSuspectOnTrap;
	private List<SnmpV3User> m_snmpV3Users;

	public void setSnmpTrapAddress(String m_snmpTrapAddress) {
		this.m_snmpTrapAddress = m_snmpTrapAddress;
	}

	public void setSnmpTrapPort(int m_snmpTrapPort) {
		this.m_snmpTrapPort = m_snmpTrapPort;
	}

	public void setM_newSuspectOnTrap(boolean m_newSuspectOnTrap) {
		this.m_newSuspectOnTrap = m_newSuspectOnTrap;
	}

	public void setSnmpV3Users(List<SnmpV3User> m_snmpV3Users) {
		this.m_snmpV3Users = m_snmpV3Users;
	}

	@Override
	public String getSnmpTrapAddress() {
		// TODO Auto-generated method stub
		return m_snmpTrapAddress;
	}

	@Override
	public int getSnmpTrapPort() {
		// TODO Auto-generated method stub
		return m_snmpTrapPort;
	}

	@Override
	public boolean getNewSuspectOnTrap() {
		// TODO Auto-generated method stub
		return m_newSuspectOnTrap;
	}

	@Override
	public List<SnmpV3User> getSnmpV3Users() {
		// TODO Auto-generated method stub
		return m_snmpV3Users;
	}

}
