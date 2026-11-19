package com.investinator.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.util.*;

/** Offline, single-use staging validation experiment. Never installed as an API authorization filter. */
public final class StagingJwtValidator {
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final boolean enabled;private final RSAPublicKey pinnedKey;private final String issuer,audience,kid;private final Clock clock;
    private final Map<String,Instant> used=new HashMap<>();
    public record Principal(String subject,String tenant,String scope,boolean production_authority) {}
    public StagingJwtValidator(boolean enabled,RSAPublicKey pinnedKey,String issuer,String audience,String kid,Clock clock){
        this.enabled=enabled;this.pinnedKey=pinnedKey;this.issuer=issuer;this.audience=audience;this.kid=kid;this.clock=clock;
        if(enabled&&(pinnedKey==null||pinnedKey.getModulus().bitLength()<2048||issuer==null||!issuer.startsWith("https://")||audience==null||audience.isBlank()||kid==null||kid.isBlank()))throw new IllegalArgumentException("Explicit pinned staging trust required");
    }
    private static long seconds(Object value){if(!(value instanceof Long||value instanceof Integer))throw new SecurityException("Integer JWT time required");return ((Number)value).longValue();}
    private Map<?,?> object(String part)throws Exception{Object value=JSON.readValue(Base64.getUrlDecoder().decode(part),Object.class);if(!(value instanceof Map<?,?> map))throw new SecurityException("JWT object required");return map;}
    public synchronized Principal validate(String token,String tenant,String scope,String proposerSubject){
        if(!enabled)throw new SecurityException("Staging identity is disabled");
        try{
            if(token==null||token.length()>16384||tenant==null||!tenant.matches("[A-Za-z0-9_.:-]{1,100}")||!"investinator:read".equals(scope))throw new SecurityException("Staging read scope only");
            String[] parts=token.split("\\.",-1);if(parts.length!=3)throw new SecurityException("JWT framing");
            var header=object(parts[0]);if(!Set.of("alg","typ","kid").containsAll(header.keySet())||!"RS256".equals(header.get("alg"))||!kid.equals(header.get("kid"))||!"JWT".equals(header.get("typ")))throw new SecurityException("Pinned algorithm/key only");
            var signature=Signature.getInstance("SHA256withRSA");signature.initVerify(pinnedKey);signature.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));if(!signature.verify(Base64.getUrlDecoder().decode(parts[2])))throw new SecurityException("JWT signature");
            var claims=object(parts[1]);Instant now=clock.instant();long exp=seconds(claims.get("exp")),iat=seconds(claims.get("iat"));
            if(exp<=now.getEpochSecond()||exp>now.plusSeconds(300).getEpochSecond()||iat>now.plusSeconds(30).getEpochSecond()||iat<now.minusSeconds(300).getEpochSecond()||exp<=iat||exp-iat>300||claims.containsKey("nbf")&&seconds(claims.get("nbf"))>now.plusSeconds(30).getEpochSecond())throw new SecurityException("JWT validity window");
            Object aud=claims.get("aud");boolean audienceMatches=aud instanceof String s?s.equals(audience):aud instanceof List<?> list&&list.size()<=10&&list.contains(audience);
            if(!issuer.equals(claims.get("iss"))||!audienceMatches||!tenant.equals(claims.get("tenant_id")))throw new SecurityException("JWT issuer/audience/tenant");
            Object roles=claims.get("roles"),scopes=claims.get("scope");if(!(roles instanceof List<?> r)||r.size()>30||!r.contains("incident-reader")||!(scopes instanceof String granted)||granted.length()>1000||!Arrays.asList(granted.split(" ")).contains(scope))throw new SecurityException("JWT role/scope");
            String subject=Objects.toString(claims.get("sub"),""),jti=Objects.toString(claims.get("jti"),"");if(!subject.matches("[A-Za-z0-9_.:@-]{1,200}")||!jti.matches("[A-Za-z0-9_.:-]{1,100}")||subject.equals(proposerSubject))throw new SecurityException("Independent subject/token ID required");
            used.entrySet().removeIf(e->!e.getValue().isAfter(now));if(used.size()>=1024||used.putIfAbsent(jti,Instant.ofEpochSecond(exp))!=null)throw new SecurityException("Replay/capacity denied");
            return new Principal(subject,tenant,scope,false);
        }catch(SecurityException denied){throw denied;}catch(Exception invalid){throw new SecurityException("Invalid staging token");}
    }
}
