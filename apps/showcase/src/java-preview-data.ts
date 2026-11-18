/** Public, explicitly synthetic examples of the Java operator contract. No backend calls. */
export type PreviewHealth = {
  service: string;
  capability: string;
  status: "HEALTHY" | "DEGRADED" | "UNKNOWN" | "NOT_CONFIGURED" | "NOT_TESTED";
  source: "SIMULATED" | "NOT_CONFIGURED" | "NOT_TESTED";
  finding: string;
  next: string;
  required: boolean;
  freshness: string;
};

export const healthSnapshot: PreviewHealth[] = [
  { service:"Java API", capability:"Actuator readiness / REST",status:"HEALTHY",source:"SIMULATED",finding:"Example: bounded HTTP readiness answered; not probed by Pages",next:"Run ./investinator doctor in local lab",required:true,freshness:"sample only" },
  { service:"PostgreSQL", capability:"JDBC transaction and schema",status:"HEALTHY",source:"SIMULATED",finding:"Example: Java Flyway owner isolated from Python Alembic",next:"Verify using disposable read/write fixture",required:true,freshness:"sample only" },
  { service:"OpenSearch", capability:"Tenant-scoped search",status:"HEALTHY",source:"SIMULATED",finding:"Example: evidence read permission is distinct from writer",next:"Run authenticated search verification",required:true,freshness:"sample only" },
  { service:"Kafka KRaft", capability:"Metadata controller quorum",status:"DEGRADED",source:"SIMULATED",finding:"Synthetic controller heartbeat delay precedes broker symptoms",next:"Inspect metadata quorum and network evidence",required:true,freshness:"sample only" },
  { service:"Fluent Bit", capability:"Log collection and forwarder",status:"HEALTHY",source:"SIMULATED",finding:"Example: collector pipeline accepted labeled fixture",next:"Inspect backlog / dropped records",required:false,freshness:"sample only" },
  { service:"OpenTelemetry", capability:"Traces and metrics",status:"HEALTHY",source:"SIMULATED",finding:"Example: trace ingestion checked against test scope",next:"Validate tenant-scoped trace retrieval",required:false,freshness:"sample only" },
  { service:"Prometheus", capability:"Health and SLO timeseries",status:"HEALTHY",source:"SIMULATED",finding:"Example: local metrics scrape, not production SLO",next:"Run /-/ready and query sample metrics",required:false,freshness:"sample only" },
  { service:"OpenAI GPT", capability:"Responses / tool calling",status:"NOT_CONFIGURED",source:"NOT_CONFIGURED",finding:"GitHub Pages does not hold an API key or call GPT",next:"Configure key and approved budget in private Java backend",required:false,freshness:"unknown" },
  { service:"Spark / YARN", capability:"Driver / executor / queues",status:"NOT_TESTED",source:"NOT_TESTED",finding:"Private cluster capabilities are not certified by this preview",next:"Authorize and test read-only diagnostic connector",required:false,freshness:"unknown" },
  { service:"HDFS / Hive", capability:"HA / HMS / HS2",status:"NOT_CONFIGURED",source:"NOT_CONFIGURED",finding:"No production NameNode, Metastore or HiveServer2 connected",next:"Configure scoped native read-only access",required:false,freshness:"unknown" },
  { service:"Kudu / NiFi", capability:"Tablet quorum / backpressure",status:"UNKNOWN",source:"SIMULATED",finding:"No fresh semantic health observation is available",next:"Treat missing evidence as UNKNOWN, not HEALTHY",required:false,freshness:"unknown" },
];

export type PreviewRule = { id:string; title:string; signal:string; mode:string; consequence:string; enabled:boolean };
export const detectionRules: PreviewRule[] = [
  {id:"R-KRAFT-QUORUM",title:"KRaft controller instability",signal:"Controller errors + heartbeat gaps",mode:"KRaft",consequence:"Kafka metadata disruption",enabled:true},
  {id:"R-LEGACY-ZK",title:"Legacy ZooKeeper session instability",signal:"Session expiration precedes broker loss",mode:"Legacy ZooKeeper only",consequence:"Kafka coordination interruption",enabled:true},
  {id:"R-KAFKA-LAG",title:"Lag and in-sync replica drift",signal:"Consumer lag + reduced ISR",mode:"Kafka",consequence:"Stream delays",enabled:true},
  {id:"R-NIFI-PRESSURE",title:"NiFi backpressure chain",signal:"Queue age + downstream PutHDFS failure",mode:"NiFi / HDFS",consequence:"Source-to-lake delivery delay",enabled:true},
  {id:"R-HDFS-HA",title:"HDFS active NameNode unavailable",signal:"HA state + RPC error",mode:"HDFS",consequence:"Downstream write outage",enabled:true},
  {id:"R-SPARK-OOM",title:"Spark executor loss",signal:"OOM + retry and scheduling changes",mode:"Spark",consequence:"Latency / stale dataset",enabled:true},
  {id:"R-KUDU-TS",title:"Kudu tablet availability",signal:"Tablet quorum and memory pressure",mode:"Kudu",consequence:"Online query failures",enabled:true},
  {id:"R-HIVE-HMS",title:"Hive Metastore availability",signal:"HMS metadata read failure vs HS2 ready",mode:"Hive",consequence:"SQL metadata lookup blocked",enabled:true},
  {id:"R-FRESHNESS",title:"Business dataset freshness",signal:"Pipeline completion vs SLO breach",mode:"All data",consequence:"Stale decisions",enabled:true},
];

export type PreviewEvent = { seconds:number; service:string; id:string; description:string; kind:"origin"|"propagation"|"business" };
export const kraftChain: PreviewEvent[] = [
  {seconds:0,service:"network",id:"KRAFT-NET-001",description:"Synthetic network packet-loss sample between controller nodes",kind:"origin"},
  {seconds:65,service:"KRaft controller",id:"KRAFT-CTRL-002",description:"Heartbeat/metadata quorum timeout",kind:"propagation"},
  {seconds:140,service:"Kafka broker",id:"KRAFT-BRK-003",description:"Broker metadata refresh failures",kind:"propagation"},
  {seconds:220,service:"Kafka consumer",id:"KRAFT-LAG-004",description:"orders.events consumer lag and fetch retries increase",kind:"propagation"},
  {seconds:390,service:"Spark streaming",id:"KRAFT-SPARK-005",description:"Micro-batches process later than scheduled",kind:"propagation"},
  {seconds:580,service:"fact_orders",id:"KRAFT-DATA-006",description:"Freshness breach impacts example revenue dashboard",kind:"business"},
];

export const legacyChain: PreviewEvent[] = [
  {seconds:0,service:"ZooKeeper",id:"LEGACY-ZK-001",description:"Representative ZooKeeper 3.9 session instability fixture",kind:"origin"},
  {seconds:65,service:"legacy Kafka broker",id:"LEGACY-BRK-002",description:"Broker reports coordination disconnect",kind:"propagation"},
  {seconds:150,service:"Kafka consumers",id:"LEGACY-CONS-003",description:"Fetch retries and lag grow",kind:"propagation"},
  {seconds:240,service:"Spark streaming",id:"LEGACY-SPK-004",description:"Ingestion delay propagated to dependent job",kind:"business"},
];

export const nifiChain: PreviewEvent[] = [
  {seconds:0,service:"HDFS",id:"NIFI-HDFS-001",description:"Simulated HDFS write interruption",kind:"origin"},
  {seconds:85,service:"NiFi PutHDFS",id:"NIFI-PUT-002",description:"Provenance reports downstream sink failures",kind:"propagation"},
  {seconds:190,service:"NiFi queue",id:"NIFI-QUEUE-003",description:"Queue age and backpressure increase",kind:"propagation"},
  {seconds:440,service:"landing dataset",id:"NIFI-DATA-004",description:"Expected arrival window missed",kind:"business"},
];

export const investigationCases = [
  {key:"kraft",title:"Kafka KRaft — originating network fault",mode:"KRaft, no ZooKeeper",origin:"Network instability affecting KRaft controller",hypotheses:[
    {name:"Network instability",rank:"Strongest synthetic hypothesis",support:["KRAFT-NET-001","KRAFT-CTRL-002"],contradiction:"Packet loss is a sample, not an independently proven physical failure."},
    {name:"Controller CPU or resource pressure",rank:"Alternative",support:["KRAFT-CTRL-002"],contradiction:"No controller CPU metric included in this example."},
    {name:"Broker disk pressure",rank:"Alternative",support:["KRAFT-BRK-003"],contradiction:"The synthetic disk fixture is not attached to this case."}
  ],events:kraftChain,checks:["Read KRaft DescribeMetadataQuorum response","Compare controller packet loss, RTT and JVM CPU","Check broker metadata and consumer lag timing"],impact:"orders.events → streaming → fact_orders → revenue reporting"},
  {key:"legacy",title:"Legacy Kafka — ZooKeeper coordination",mode:"Legacy ZooKeeper-backed Kafka only",origin:"ZooKeeper session instability",hypotheses:[
    {name:"ZooKeeper session instability",rank:"Strongest synthetic hypothesis",support:["LEGACY-ZK-001","LEGACY-BRK-002"],contradiction:"Actual Kafka broker and ZK cluster were not connected."},
    {name:"Broker network partition",rank:"Alternative",support:["LEGACY-BRK-002"],contradiction:"Missing packet-level network evidence."}
  ],events:legacyChain,checks:["Verify broker metadata mode before any ZK diagnostic","Read authenticated ZooKeeper status and ACL-limited znode health","Compare broker session logs and ZK quorum events"],impact:"Broker coordination → consumers → stream latency"},
  {key:"nifi",title:"NiFi — upstream-looking sink bottleneck",mode:"NiFi → HDFS",origin:"HDFS write interruption",hypotheses:[
    {name:"HDFS downstream storage issue",rank:"Strongest synthetic hypothesis",support:["NIFI-HDFS-001","NIFI-PUT-002"],contradiction:"No real NameNode or DataNode checks are present."},
    {name:"NiFi processor scheduling",rank:"Alternative",support:["NIFI-QUEUE-003"],contradiction:"Backpressure occurs after PutHDFS failures."}
  ],events:nifiChain,checks:["Compare NameNode HA and HDFS data-path reads","Inspect NiFi queue age, processor backpressure and provenance","Validate destination ACLs and safe write permissions"],impact:"NiFi ingestion queue → landing dataset freshness"},
] as const;

export const releaseGates = [
  {id:"P14-01",name:"OIDC and independent enterprise identities",status:"BLOCKED"},
  {id:"P14-02",name:"Real connector Kerberos/TLS/tenancy certification",status:"BLOCKED"},
  {id:"P14-03",name:"Encrypted offsite restore, PITR and HA/DR",status:"BLOCKED"},
  {id:"P14-04",name:"Approved live GPT budget and durable financial caps",status:"BLOCKED"},
  {id:"P14-05",name:"Independent human gold-set RCA review",status:"BLOCKED"},
  {id:"P14-06",name:"Enterprise perimeter, ingress and egress review",status:"BLOCKED"},
  {id:"P14-07",name:"Retention, privacy, legal hold and audit policy",status:"BLOCKED"},
  {id:"P14-08",name:"Optional dependency and container image security certification",status:"BLOCKED"},
  {id:"P14-09",name:"Staging performance and SLO measurements",status:"CONDITIONAL"},
  {id:"P14-10",name:"Demo application security hardening tests",status:"LOCAL_PASS"},
];
