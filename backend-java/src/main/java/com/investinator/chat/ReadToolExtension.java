package com.investinator.chat;
import com.openai.models.responses.FunctionTool;
import java.util.*;
/** Trusted plugin SPI. Implementations are read-only; registration is not model-controlled. */
public interface ReadToolExtension {
    List<FunctionTool> definitions();
    Map<String,Object> execute(String name,Map<String,Object> args,ReadTools.Context context);
    default String policyEpoch(String tenant){return "0";}
}
