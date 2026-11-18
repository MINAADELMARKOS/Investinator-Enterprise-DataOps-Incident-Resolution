package com.investinator.security;
/** Deployment-owned policy scaffolding; no model/client text can create a data-use approval. */
public final class DataUsePolicy {
    private DataUsePolicy() {}
    public record Decision(boolean allowed,String reason) {}
    public static Decision modelTransfer(boolean ownerApprovalConfigured,String classification,String sourceMode){
        if(!ownerApprovalConfigured)return new Decision(false,"DATA_TRANSFER_NOT_APPROVED");
        if(!java.util.Set.of("SYNTHETIC","PUBLIC_LICENSED").contains(classification))return new Decision(false,"DATA_CLASS_NOT_CERTIFIED");
        if(!java.util.Set.of("SIMULATED","NOT_TESTED").contains(sourceMode))return new Decision(false,"LIVE_DATA_NOT_CERTIFIED");
        return new Decision(true,"CONFIGURATION_ONLY_OWNER_CERTIFICATION_STILL_REQUIRED");
    }
    public static Decision deletion(boolean legalHold,boolean retentionApproved){
        return new Decision(false,legalHold?"LEGAL_HOLD":"NO_APPROVED_DELETION_EXECUTOR");
    }
}
