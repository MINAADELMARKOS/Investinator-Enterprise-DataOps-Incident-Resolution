package com.investinator.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DataUsePolicyTest {
    @Test void absentApprovalUnknownProtectedAndLiveDataFailClosed(){assertFalse(DataUsePolicy.modelTransfer(false,"SYNTHETIC","SIMULATED").allowed());for(String c:new String[]{"UNKNOWN","PROTECTED_LOG","PERSONAL_DATA","RUNBOOK_UNLICENSED"})assertFalse(DataUsePolicy.modelTransfer(true,c,"SIMULATED").allowed());assertFalse(DataUsePolicy.modelTransfer(true,"PUBLIC_LICENSED","LIVE").allowed());}
    @Test void explicitlyConfiguredSyntheticTransferIsNotAnEnterpriseCertificate(){var d=DataUsePolicy.modelTransfer(true,"SYNTHETIC","SIMULATED");assertTrue(d.allowed());assertTrue(d.reason().contains("CERTIFICATION_STILL_REQUIRED"));}
    @Test void legalHoldAndUnapprovedRetentionNeverExecuteDeletion(){for(boolean hold:new boolean[]{false,true})for(boolean retention:new boolean[]{false,true})assertFalse(DataUsePolicy.deletion(hold,retention).allowed());assertEquals("LEGAL_HOLD",DataUsePolicy.deletion(true,true).reason());}
}
