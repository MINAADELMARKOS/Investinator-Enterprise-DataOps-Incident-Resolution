package com.investinator.health;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.util.*;
class ProbeHttpTest {
    @Test void authenticatedGetPreservesStatusAndRefusesRedirects() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/read",exchange->{assertEquals("GET",exchange.getRequestMethod());int code="Bearer test-only".equals(exchange.getRequestHeaders().getFirst("Authorization"))?200:403;byte[] body="{\"version\":\"1\"}".getBytes();exchange.sendResponseHeaders(code,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.createContext("/redirect",exchange->{exchange.getResponseHeaders().set("Location","http://outside.invalid/");exchange.sendResponseHeaders(302,-1);exchange.close();});server.start();
        try{String base="http://127.0.0.1:"+server.getAddress().getPort();var http=new ProbeHttp();assertEquals(403,http.get(base,"/read",Map.of()).status());assertEquals("1",http.get(base,"/read",Map.of("Authorization","Bearer test-only")).json().get("version"));assertEquals(302,http.get(base,"/redirect",Map.of()).status());assertThrows(IllegalArgumentException.class,()->http.get("http://user:secret@127.0.0.1","/read",Map.of()));}finally{server.stop(0);}
    }
    @Test void responseSizeIsBounded() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/large",e->{byte[] body=new byte[1048577];e.sendResponseHeaders(200,body.length);try{e.getResponseBody().write(body);}finally{e.close();}});server.start();try{assertThrows(java.io.IOException.class,()->new ProbeHttp().get("http://127.0.0.1:"+server.getAddress().getPort(),"/large",Map.of()));}finally{server.stop(0);}
    }
}
