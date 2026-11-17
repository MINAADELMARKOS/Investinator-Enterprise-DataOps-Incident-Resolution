package com.investinator.persistence;
import static com.investinator.domain.Json.*;
import java.util.*;
import java.sql.*;
import java.time.*;
import java.util.function.Supplier;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import com.investinator.api.ApiFailure;
@Repository
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class Store {
    public final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public Store(JdbcTemplate jdbc,TransactionTemplate tx) {this.jdbc=jdbc;this.tx=tx;}
    public <T> T read(Supplier<T> work) {return tx.execute(status -> work.get());}
    public <T> T write(String tenant,Supplier<T> work) {
        return tx.execute(status -> {
            jdbc.query("select pg_advisory_xact_lock(hashtextextended(?,0))",r -> {},tenant);
            return work.get();
        });
    }
    public Object command(String tenant,String route,String key,String hash,Supplier<Object> work) {
        if (key!=null && !key.matches("[A-Za-z0-9._:-]{1,128}")) throw new ApiFailure(422,"Invalid Idempotency-Key");
        return write(tenant,() -> {
            if (key==null) return work.get();
            var previous=select(Table.REQUEST_KEYS,tenant,"route=? and key=?",route,key);
            if (!previous.isEmpty()) {
                if (!hash.equals(previous.getFirst().get("request_hash"))) throw new ApiFailure(409,"Idempotency-Key was used for another request");
                return previous.getFirst().get("response");
            }
            Object result=work.get();
            insert(Table.REQUEST_KEYS,tenant,obj("route",route,"key",key,"request_hash",hash,"response",result));
            return result;
        });
    }
    public List<Map<String,Object>> all(Table table,String tenant) {return select(table,tenant,"true");}
    // Predicates are internal constant SQL only; all request values are JDBC parameters.
    public List<Map<String,Object>> select(Table table,String tenant,String predicate,Object... values) {
        var args=new ArrayList<Object>();args.add(tenant);args.addAll(Arrays.asList(values));
        String order=table.columns.contains("observed_at")?"observed_at desc":table.columns.contains("created_at")?"created_at desc":"id";
        return jdbc.query("select * from "+table.sql+" where tenant_id=? and ("+predicate+") order by "+order+" limit 5000",
            (row,index) -> {
                var result=new LinkedHashMap<String,Object>();var meta=row.getMetaData();
                for(int i=1;i<=meta.getColumnCount();i++) {
                    Object value=row.getObject(i);
                    if(value instanceof PGobject p && (p.getType().equals("json")||p.getType().equals("jsonb"))) value=decode(p.getValue());
                    if(value instanceof Timestamp t)value=t.toInstant().toString();
                    if(value instanceof OffsetDateTime d)value=d.toInstant().toString();
                    result.put(meta.getColumnName(i),value);
                }
                return result;
            },args.toArray());
    }
    public Map<String,Object> one(Table table,String tenant,String id) {
        return select(table,tenant,"id=?",id).stream().findFirst().orElseThrow(() -> new ApiFailure(404,"Record not found"));
    }
    public void insert(Table table,String tenant,Map<String,Object> supplied) {
        var row=new LinkedHashMap<>(supplied);row.put("tenant_id",tenant);
        if(table.columns.contains("id"))row.putIfAbsent("id",id());
        if(table.columns.contains("created_at"))row.putIfAbsent("created_at",now());
        if(table.columns.contains("updated_at"))row.putIfAbsent("updated_at",now());
        var columns=new ArrayList<>(row.keySet());
        if(!table.columns.containsAll(columns)) throw new IllegalArgumentException("Unknown repository column");
        String placeholders=String.join(",",Collections.nCopies(columns.size(),"?"));
        jdbc.update("insert into "+table.sql+" ("+String.join(",",columns)+") values ("+placeholders+")",
            columns.stream().map(k -> parameter(k,row.get(k))).toArray());
    }
    public void update(Table table,String tenant,String id,Map<String,Object> values) {
        if(!table.columns.containsAll(values.keySet()) || values.containsKey("tenant_id") || values.containsKey("id"))
            throw new IllegalArgumentException("Invalid repository update");
        var args=new ArrayList<Object>();var fields=new ArrayList<String>();
        values.forEach((k,v)->{fields.add(k+"=?");args.add(parameter(k,v));});args.add(tenant);args.add(id);
        if(jdbc.update("update "+table.sql+" set "+String.join(",",fields)+" where tenant_id=? and id=?",args.toArray())!=1)
            throw new ApiFailure(404,"Record not found");
    }
    private Object parameter(String column,Object value) {
        if(value==null)return null;
        if(column.equals("payload")||column.equals("response")) {
            try{var p=new PGobject();p.setType("json");p.setValue(encode(value));return p;}
            catch(SQLException e){throw new IllegalArgumentException("Invalid JSON",e);}
        }
        if(column.endsWith("_at"))return Timestamp.from(Instant.parse(value.toString()));
        return value instanceof Enum<?> e?e.name():value;
    }
}
