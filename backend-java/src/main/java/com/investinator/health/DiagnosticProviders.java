package com.investinator.health;
import java.util.List;
/** Trusted-code extension point, never populated from a request or model output. */
public interface DiagnosticProviders {List<HealthProbe> probes();}
