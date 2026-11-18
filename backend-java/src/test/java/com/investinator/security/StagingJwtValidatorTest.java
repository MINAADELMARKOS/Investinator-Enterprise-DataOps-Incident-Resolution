package com.investinator.security;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.util.*;
import static com.investinator.domain.Json.*;
class StagingJwtValidatorTest {
    static KeyPair keys;final Clock clock=Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"),ZoneOffset.UTC);
    @BeforeAll static void keys()throws Exception{var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);keys=generator.generateKeyPair();}
    Map<String,Object> claims(){return obj("iss","https://fixture-idp.invalid","aud","investinator-staging","sub","reviewer","tenant_id","demo","scope","investinator:read","roles",List.of("incident-reader"),"iat",clock.instant().getEpochSecond(),"exp",clock.instant().plusSeconds(120).getEpochSecond(),"jti",UUID.randomUUID().toString());}
    StagingJwtValidator validator(boolean enabled){return new StagingJwtValidator(enabled,(RSAPublicKey)keys.getPublic(),"https://fixture-idp.invalid","investinator-staging","fixture-key",clock);}
    String token(Map<String,Object> claims,String algorithm)throws Exception{var encoder=Base64.getUrlEncoder().withoutPadding();String signed=encoder.encodeToString(encode(obj("alg",algorithm,"typ","JWT","kid","fixture-key")).getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+encoder.encodeToString(encode(claims).getBytes(java.nio.charset.StandardCharsets.UTF_8));var signature=Signature.getInstance("SHA256withRSA");signature.initSign(keys.getPrivate());signature.update(signed.getBytes(java.nio.charset.StandardCharsets.US_ASCII));return signed+"."+encoder.encodeToString(signature.sign());}
    @Test void disabledAndWriteScopesFailClosed()throws Exception{String token=token(claims(),"RS256");assertThrows(SecurityException.class,()->validator(false).validate(token,"demo","investinator:read",null));assertThrows(SecurityException.class,()->validator(true).validate(token,"demo","investinator:write",null));}
    @Test void validPinnedReadHasNoProductionAuthorityAndReplayIsDenied()throws Exception{var v=validator(true);String token=token(claims(),"RS256");assertFalse(v.validate(token,"demo","investinator:read",null).production_authority());assertThrows(SecurityException.class,()->v.validate(token,"demo","investinator:read",null));}
    @Test void issuerAudienceTenantRoleScopeAndExpiryFailuresAreDenied()throws Exception{for(var replacement:List.of(obj("iss","https://wrong.invalid"),obj("aud","other"),obj("tenant_id","other"),obj("roles",List.of("admin")),obj("scope","write"),obj("exp",clock.instant().minusSeconds(1).getEpochSecond()),obj("iat",clock.instant().plusSeconds(40).getEpochSecond()))){var c=claims();c.putAll(replacement);String token=token(c,"RS256");assertThrows(SecurityException.class,()->validator(true).validate(token,"demo","investinator:read",null));}}
    @Test void forgedSignatureAlgorithmAndSamePersonAreDenied()throws Exception{String valid=token(claims(),"RS256"),none=token(claims(),"none");assertThrows(SecurityException.class,()->validator(true).validate(valid.substring(0,valid.length()-12)+"aaaaaaaaaaaa","demo","investinator:read",null));assertThrows(SecurityException.class,()->validator(true).validate(none,"demo","investinator:read",null));assertThrows(SecurityException.class,()->validator(true).validate(valid,"demo","investinator:read","reviewer"));}
}
