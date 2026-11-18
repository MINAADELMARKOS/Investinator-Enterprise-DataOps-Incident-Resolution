package com.investinator.connectors;
import com.investinator.health.HealthCheckResult;
import java.util.*;
import static com.investinator.domain.Json.*;
/** Certification evidence inventory, not a certificate. Health/fixture success cannot authorize native ingestion. */
public final class ConnectorCertification {
    private ConnectorCertification() {}
    public static Map<String,Object> assess(String tenant,List<DiagnosticConnector.Descriptor> descriptors,List<HealthCheckResult> checks){
        if(tenant==null||!tenant.matches("[A-Za-z0-9_.:-]{1,100}"))throw new IllegalArgumentException("Server tenant required");
        var rows=new ArrayList<Map<String,Object>>();
        for(var descriptor:descriptors){var check=checks.stream().filter(c->c.service().equals(descriptor.service())).findFirst();
            String health=check.map(c->c.status().name()).orElse("NOT_TESTED");
            rows.add(obj("tenant_id",tenant,"service",descriptor.service(),"protocol",descriptor.protocol(),"version_contract",descriptor.version_contract(),"auth_contract",descriptor.authentication(),"supported_read_capabilities",descriptor.supported().stream().map(Enum::name).sorted().toList(),"health",health,"transport_source_mode",check.map(c->c.source_mode().name()).orElse("NOT_TESTED"),"data_source_mode",check.map(c->Objects.toString(c.details().get("data_source_mode"),"UNKNOWN")).orElse("UNKNOWN"),"observed_version",check.map(c->Objects.toString(c.details().get("version"),"UNKNOWN")).orElse("UNKNOWN"),"enterprise_status",health.equals("NOT_CONFIGURED")?"NOT_CONFIGURED":"NOT_CERTIFIED","native_ingestion_authorized",false,"required_external_evidence",List.of("representative target and exact version/mode","TLS trust and hostname verification","Kerberos/SPNEGO/mTLS identity and rotation","denied writes and foreign tenant/source reads","independent integration/security owner review")));
        }
        return obj("tenant_id",tenant,"rows",rows,"production_certificate","BLOCKED","certification_owner","integration/security leads (proposed role)","native_ingestion_authorized",false,"interpretation","Local health or mocked protocol success never proves private target behavior");
    }
}
