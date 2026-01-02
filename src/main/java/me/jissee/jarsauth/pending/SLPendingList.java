package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.pending.base.PendingList;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class SLPendingList extends PendingList {
    public SLPendingList(MinecraftServer server) {
        super(server, false);
    }

    @Override
    protected String generateRandom() {
        return "";
    }

    @Override
    protected String calculateExpected(UUID userId, String random) throws Exception {
        onVerificationResponse(userId, "none");

        return "true";
    }

    @Override
    protected boolean compare(String expected, String actual) {
        return false;
    }

    @Override
    protected void sendInfoToPlayer(UUID userId, String random) {

    }

    @Override
    protected void notifyFailure(UUID userId, String reason) {

    }

    @Override
    protected String formatReason(FailureType type, Exception e) {
        return "";
    }

    @Override
    protected long getInterval() {
        return 0;
    }

    @Override
    protected long getTimeout() {
        return 0;
    }
}
