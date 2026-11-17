package com.investinator.connectors;
import com.investinator.health.HealthProbe;
import java.util.*;
/** Trusted backend SPI. No caller-supplied endpoint, SQL, command, or mutation capability. */
public interface DiagnosticConnector extends HealthProbe {
    enum Capability { health, metrics, logs, topology, config, metadata, readOnlyChecks }
    record Descriptor(String service,String protocol,String version_contract,String authentication,
        Set<Capability> supported,List<String> limitations) {}
    Descriptor descriptor();
    default String capability(){return "authenticated-version-semantic-read";}
}
