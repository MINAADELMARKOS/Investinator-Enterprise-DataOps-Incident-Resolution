package com.investinator.correlation;
import java.time.*;
import java.util.*;
/** Immutable observations of topology, never model-generated dependency assertions. */
public record DependencyGraph(String graph_id,int revision,String source_mode,String metadata_mode,String platform_version,
    List<Node> nodes,List<Edge> edges) {
    public enum NodeType { SERVICE,CLUSTER,HOST,NETWORK,BROKER,CONTROLLER,QUORUM,TOPIC,CONSUMER_GROUP,SPARK_JOB,PIPELINE,TABLE,DATASET,DASHBOARD,BUSINESS_PROCESS,ZOOKEEPER }
    public enum EdgeType { DEPENDS_ON,HOSTED_ON,FEEDS,WRITES_TO,USES_QUORUM,PRODUCES_TO,CONNECTS_TO }
    public record Node(String id,NodeType type,String name) {}
    public record Edge(String id,String from,String to,EdgeType type,String source,double confidence,String discovered_at,String valid_from,String valid_to,String provenance) {}
    public DependencyGraph {
        nodes=List.copyOf(nodes);edges=List.copyOf(edges);
        if(graph_id==null||!graph_id.matches("[A-Za-z0-9_.-]{1,100}")||revision<1||nodes.size()>256||edges.size()>1000||!Set.of("SIMULATED","LIVE").contains(source_mode)||!Set.of("KRAFT","LEGACY_ZOOKEEPER","UNKNOWN").contains(metadata_mode)||platform_version==null||platform_version.isBlank())throw new IllegalArgumentException("Invalid graph bounds/provenance");
        var ids=new HashSet<String>();for(var n:nodes)if(n.type==null||n.id==null||!n.id.matches("[A-Za-z0-9_.:-]{1,150}")||n.name==null||n.name.length()>200||!ids.add(n.id))throw new IllegalArgumentException("Invalid/duplicate graph node");
        var edgeIds=new HashSet<String>();for(var e:edges){
            if(e.type==null||!ids.contains(e.from)||!ids.contains(e.to)||e.from.equals(e.to)||e.id==null||!edgeIds.add(e.id)||e.source==null||e.source.isBlank()||e.provenance==null||e.provenance.isBlank()||!Double.isFinite(e.confidence)||e.confidence<0||e.confidence>1)throw new IllegalArgumentException("Unverified/dangling graph edge");
            Instant from=Instant.parse(e.valid_from),discovered=Instant.parse(e.discovered_at);if(e.valid_to!=null&&!Instant.parse(e.valid_to).isAfter(from))throw new IllegalArgumentException("Invalid validity range");
            if(discovered.isBefore(from.minus(Duration.ofDays(3650))))throw new IllegalArgumentException("Invalid discovered time");
        }
        var nodeSnapshot=nodes;
        if(metadata_mode.equals("KRAFT")&&nodes.stream().anyMatch(n->n.type==NodeType.ZOOKEEPER))throw new IllegalArgumentException("KRaft graph cannot invent ZooKeeper dependency");
        if(metadata_mode.equals("LEGACY_ZOOKEEPER")&&edges.stream().anyMatch(e->e.type==EdgeType.USES_QUORUM&&nodeSnapshot.stream().anyMatch(n->n.id.equals(e.to)&&(n.type==NodeType.CONTROLLER||n.type==NodeType.QUORUM))))throw new IllegalArgumentException("Legacy graph cannot invent KRaft quorum dependency");
    }
    public List<Edge> at(Instant instant){return edges.stream().filter(e->!Instant.parse(e.discovered_at).isAfter(instant)&&!Instant.parse(e.valid_from).isAfter(instant)&&(e.valid_to==null||Instant.parse(e.valid_to).isAfter(instant))).toList();}
    public Set<String> downstream(String root,Instant at) {
        var adjacency=new HashMap<String,List<String>>();for(var e:at(at)) {
            boolean flow=Set.of(EdgeType.FEEDS,EdgeType.WRITES_TO,EdgeType.PRODUCES_TO).contains(e.type);
            adjacency.computeIfAbsent(flow?e.from:e.to,k->new ArrayList<>()).add(flow?e.to:e.from);
        }
        var visited=new LinkedHashSet<String>();var queue=new ArrayDeque<String>();visited.add(root);queue.add(root);
        while(!queue.isEmpty())for(String child:adjacency.getOrDefault(queue.remove(),List.of()))if(visited.add(child))queue.add(child);
        return Collections.unmodifiableSet(visited);
    }
}
