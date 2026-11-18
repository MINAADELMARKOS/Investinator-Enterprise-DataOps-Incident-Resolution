package com.investinator.chat;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component
public class StateCipher {
    private final byte[] key;
    public StateCipher(Environment env){String value=env.getProperty("INVESTINATOR_CHAT_STATE_KEY","");key=value.isBlank()?null:Base64.getDecoder().decode(value);if(key!=null&&key.length!=32)throw new IllegalArgumentException("Chat state key must be 32 bytes, base64 encoded");}
    public boolean configured(){return key!=null;}
    public String encrypt(String tenant,String conversation,String text){try{byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD((tenant+":"+conversation).getBytes(StandardCharsets.UTF_8));byte[] encrypted=cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));var result=new byte[nonce.length+encrypted.length];System.arraycopy(nonce,0,result,0,nonce.length);System.arraycopy(encrypted,0,result,nonce.length,encrypted.length);return Base64.getEncoder().encodeToString(result);}catch(Exception e){throw new IllegalStateException("Private state encryption failed");}}
    public String decrypt(String tenant,String conversation,String value){try{byte[] bytes=Base64.getDecoder().decode(value);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Arrays.copyOf(bytes,12)));cipher.updateAAD((tenant+":"+conversation).getBytes(StandardCharsets.UTF_8));return new String(cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Private state authentication failed");}}
}
