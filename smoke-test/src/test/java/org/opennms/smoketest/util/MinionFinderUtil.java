package org.opennms.smoketest.util;

import java.util.ArrayList;

import org.opennms.smoketest.minion.AbstractSyslogTestCase;
import org.opennms.test.system.api.NewTestEnvironment.ContainerAlias;

/**
 * @author ps044221
 * Utility of checking which minion container is running
 */
public class MinionFinderUtil extends AbstractSyslogTestCase{
	
	public ContainerAlias getMinionAlias() {

		ContainerAlias containerAlias = null;

		ArrayList<ContainerAlias> minionContainerList = new ArrayList<ContainerAlias>();
		minionContainerList.add(ContainerAlias.MINION);
		minionContainerList.add(ContainerAlias.MINION_OTHER_LOCATION);
		minionContainerList.add(ContainerAlias.MINION_SAME_LOCATION);

		for (int i = 0; i < 3; i++) {
			try {
				testEnvironment.getServiceAddress(
						(ContainerAlias) minionContainerList.get(i), 8201);
				containerAlias = (ContainerAlias) minionContainerList.get(i);
			} catch (Exception e) {
				continue;
			}
		}

		return containerAlias;
	}

}
