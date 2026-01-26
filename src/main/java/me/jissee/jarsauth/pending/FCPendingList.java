package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.Codec;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.FileSelector;
import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.AuthRuleEntry;
import me.jissee.jarsauth.data.model.FileList;
import me.jissee.jarsauth.data.service.AccProfileService;
import me.jissee.jarsauth.data.service.AuthRuleService;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.event.EventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;


public class FCPendingList extends AbstractPendingList<List<String>> {

    public FCPendingList(MinecraftServer server) {
        super(server, true);
    }

    @Override
    protected List<String> calculateExpected(UUID userId, String random) throws Exception {
        AccProfileService service = dataManager.getService(AccProfileService.class);
        AuthRuleService ruleService = dataManager.getService(AuthRuleService.class);
        List<String> clientGroups = service.getRegisteredAccGroupNames();
        List<String> results = new ArrayList<>();
        for(String groupName : clientGroups){
            FileSelector selector = new FileSelector("./");
            AcceptedDetail detail = service.getGroup(groupName);
            selector.setDataSource(detail);

            AuthRuleEntry entry = ruleService.getFlattenRuleEntry(detail);
            entry.rules().forEach(selector::addFilter);

            FileList fileList = selector.getFileList();
            String hash = fileList.hash(detail, random);
            results.add(hash);
        }
        return results;
    }

    @Override
    protected boolean compare(UserContext ctx, List<String> expected, List<String> actual) {
        if(expected.isEmpty()){
            return true;
        }
        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> actualSet =
                actual.stream().map(sha256 -> {
                    try {
                        byte[] byteShaEnc = Codec.hexToBytes(sha256);
                        byte[] byteSha = Codec.decrypt(byteShaEnc, Codec.getPrivateKey(ctx.key));
                        return Codec.bytesToHex(byteSha);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet());

        expectedSet.retainAll(actualSet);
        return !expectedSet.isEmpty();
    }

    @Override
    protected void sendInfoToPlayer(UserContext ctx, String random) {
        ServerPlayer player = server.getPlayerList().getPlayer(ctx.userId);

        if (player == null) return;
        int flag = -514;
        PlayerTeam team = new PlayerTeam(new Scoreboard(), "!!$%!$" + flag);
        Collection<String> rules = team.getPlayers();
        AuthRuleService service = dataManager.getService(AuthRuleService.class);
        String json = service.getAllRulesAsJson();
        rules.add(json);
        rules.add("[" + random + "]");
        try {
            PublicKey key = Codec.getKey();
            ctx.key = key;
            byte[] keyArray = key.getEncoded();
            rules.add("<" + Codec.bytesToHex(keyArray) + ">");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ClientboundSetPlayerTeamPacket packet =
                ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
        player.connection.send(packet);
    }

    @Override
    protected void notifyFailure(UUID userId, String reason) {
        Component reasonComponent;
        if(reason.startsWith(".")){
            reasonComponent = Component.literal(reason.substring(1));
        }else{
            reasonComponent = Component.translatable(reason);
        }
        ServerPlayer player = server.getPlayerList().getPlayer(userId);
        EventHandler.addPlayerToBeRemove(player, reasonComponent, 0);
    }

    @Override
    protected String formatReason(FailureType type, Exception e) {
        if(e != null) {
            return "." + e.getMessage();
        }else{
            return "fc." + type.key();
        }
    }

    @Override
    protected long getInterval() {
        ConfigService service = dataManager.getService(ConfigService.class);
        return service.getValue(ConfigKey.FILE_CHECKSUM_INTERVAL);
    }

    @Override
    protected long getTimeout() {
        ConfigService service = dataManager.getService(ConfigService.class);
        return service.getValue(ConfigKey.FILE_CHECKSUM_TIMEOUT);
    }
}
