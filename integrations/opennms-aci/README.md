
## Running

```
feature:install json-simple
install mvn:org.opennms/opennms-config-model/21.0.1-SNAPSHOT
install mvn:org.opennms.aci/opennms-aci-config/21.0.1-SNAPSHOT
install mvn:org.opennms.aci/aci-rest/21.0.1-SNAPSHOT
install mvn:org.opennms.aci/org.opennms.aci.rpc.commands/21.0.1-SNAPSHOT
install mvn:org.opennms.aci/opennms-aci.main-module/21.0.1-SNAPSHOT
install mvn:org.opennms.aci/opennms-aci-provision/21.0.1-SNAPSHOT

OR

feature:repo-add mvn:org.opennms.aci/opennms-aci/21.0.1-SNAPSHOT/xml/features
feature:install opennms-aci
```

## Sample Provision import-url-resourc values
```
requisition://aci?apic-url=https://7.192.80.10/api/node/class/topSystem.json%26k1=v1%26k2=v2%26location=Default%26username=svcOssAci
requisition://aci?cluster-name=CTC-KC-VIII
requisition://aci?cluster-name=CTC-LS-VI
```
