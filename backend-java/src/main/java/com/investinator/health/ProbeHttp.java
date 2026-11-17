package com.investinator.health;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import com.investinator.domain.Json;
public class ProbeHttp {
    private final HttpClient client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
    public record Reply(int status,String body,Map<String,List<String>> headers) {public Reply(int status,String body){this(status,body,Map.of());}public Map<String,Object> json(){return Json.map(Json.decode(body));}}
    public Reply get(String base,String path,Map<String,String> headers) throws Exception {
        URI root=URI.create(base);
        if(root.getHost()==null||root.getUserInfo()!=null||root.getQuery()!=null||root.getFragment()!=null||!Set.of("http","https").contains(root.getScheme()))throw new IllegalArgumentException("Invalid configured diagnostic endpoint");
        URI uri=URI.create(base.replaceAll("/$","")+path);
        var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET();headers.forEach(request::header);
        var response=client.send(request.build(),info->new LimitedBody());
        return new Reply(response.statusCode(),new String(response.body(),StandardCharsets.UTF_8),response.headers().map());
    }
    private static class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(Flow.Subscription value){subscription=value;value.request(1);}
        public void onNext(List<ByteBuffer> chunks){try{for(var chunk:chunks){if(bytes.size()+chunk.remaining()>1048576){subscription.cancel();result.completeExceptionally(new IOException("Diagnostic response limit"));return;}byte[] data=new byte[chunk.remaining()];chunk.get(data);bytes.write(data);}subscription.request(1);}catch(Exception e){subscription.cancel();result.completeExceptionally(e);}}
        public void onError(Throwable error){result.completeExceptionally(error);}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
