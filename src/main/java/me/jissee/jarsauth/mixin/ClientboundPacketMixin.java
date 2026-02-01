package me.jissee.jarsauth.mixin;

import com.google.gson.*;
import me.jissee.jarsauth.Codec;
import me.jissee.jarsauth.ThreadExecutor;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.FileSelector;
import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.FileList;
import me.jissee.jarsauth.data.service.ClientDataService;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Mixin(value = ClientboundSetPlayerTeamPacket.class, priority = Integer.MAX_VALUE)
public class ClientboundPacketMixin {
    private static final int replaceTarget1 = -114;   // fc record    -> send archive
    private static final int replaceTarget2 = -514;   // fc auth info -> send auth
    private static final int replaceTarget3 = -0114;  // ca register  -> pk / store id
    private static final int replaceTarget4 = -0514;  // ca auth info -> send auth
    private static final Gson gson = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("Client Packet Handler");
    private static PublicKey publicKey;


    @Shadow @Final private Collection<String> players;
    @Shadow @Final private String name;

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = {@At("HEAD")}, cancellable = true)
    private void handle(ClientGamePacketListener p_133310_, CallbackInfo ci) throws Exception {
        if(Assert.assertFalse(true)) return;
        ClientPacketListener er = (ClientPacketListener) p_133310_;
        boolean isDefault = false;
        String teamName = name;
        int flag;
        if(teamName.startsWith("!!$%!$")){
            teamName = teamName.substring(6);
            flag = Integer.parseInt(teamName);
        }else{
            return;
        }
        PublicKey serverPublicKey = null;
        String random = "";
        String payload = "";
        for(String player : players){
            if(player.startsWith("<") && player.endsWith(">")){
                String keyStr = player.substring(1, player.length()-1);
                byte[] keyArray = Codec.hexToBytes(keyStr);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyArray);
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    serverPublicKey = keyFactory.generatePublic(keySpec);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }else if(player.startsWith("[") && player.endsWith("]")){
                random = player.substring(1);
                random = random.substring(0, random.length()-1);
            }else{
                payload = player;
            }
        }
        String playerName = Objects.requireNonNull(Minecraft.getInstance().player).getName().getString();
        if(flag == replaceTarget1) {
            Runnable sendArchiveTask = () -> {
                File defaultFile = new File("");
                defaultFile = defaultFile.getAbsoluteFile();

                String clientRootDir = defaultFile.getAbsolutePath();
                if (!clientRootDir.endsWith(File.separator)) {
                    if (clientRootDir.endsWith(".")) {
                        clientRootDir = clientRootDir.substring(0, clientRootDir.length() - 2);
                    } else {
                        clientRootDir = clientRootDir + File.separator;
                    }
                }


                AcceptedDetail detail = null;
                try {
                    FileSelector selector = new FileSelector(clientRootDir);
                    detail = selector.scan("");
                } catch (Exception e) {
                    LOGGER.error("Error while reading native files: ", e);
                }
                if (detail == null) {
                    return;
                }

                Map<String, String> files = detail.files();
                List<String> folders = detail.folders();


                int i = 0;
                List<List<String>> strPacks = new ArrayList<>();
                List<String> strl = new ArrayList<>();

                for (String folder : folders) {
                    folder = folder.replace('\\', '/');
                    if (i == 100) {
                        strPacks.add(strl);
                        LOGGER.info("adding info a");
                        i = 0;
                        strl = new ArrayList<>();
                    }
                    String key = folder;
                    String value = "folder";
                    strl.add(key);
                    strl.add(value);
                    i++;
                }
                for (String file : files.keySet()) {
                    file = file.replace('\\', '/');
                    if (i == 100) {
                        strPacks.add(strl);
                        LOGGER.info("adding info b");
                        i = 0;
                        strl = new ArrayList<>();
                    }
                    String key = file;
                    String value = files.get(key);
                    if (value == null) {
                        LOGGER.warn("Cannot access file {}", file);
                        value = "UNKNOWN";
                    }
                    strl.add(key);
                    strl.add(value);
                    i++;
                }
                if (!strl.isEmpty()) {
                    strPacks.add(strl);
                    LOGGER.info("adding info c");
                }
                try {
                    int count = 0;
                    for (List<String> pack : strPacks) {
                        count += pack.size();
                    }
                    count = count / 2;
                    for (List<String> pack : strPacks) {
                        ServerboundEditBookPacket packet =
                                new ServerboundEditBookPacket(-810, pack, Optional.of(String.valueOf(count)));
                        er.send(packet);
                    }
                } catch (Exception e) {
                    LOGGER.error("EXCEPTION THROWN: ", e);
                }
            };
            ThreadExecutor.getInstance().execute(sendArchiveTask);
        }else if (flag == replaceTarget2) {
            String finalRandom = random;
            PublicKey finalPubKey = serverPublicKey;
            String finalPayload = payload;
            Runnable sendAuthTask = () -> {
                JsonObject jsonObject = JsonParser.parseString(finalPayload).getAsJsonObject();
                List<String> results = new ArrayList<>();
                for (String key : jsonObject.keySet()) {
                    JsonArray jsonArray = jsonObject.get(key).getAsJsonArray();
                    LoggerFactory.getLogger("Received Auth Profile").debug("key: {}, value: {}", key, jsonArray);
                    FileSelector selector = new FileSelector(".");
                    for (JsonElement element : jsonArray) {
                        String rule = element.getAsString();
                        selector.addFilter(rule);
                    }
                    FileList fileList = null;
                    try {
                        fileList = selector.getFileList();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String hash = fileList.hash("./", finalRandom);
                    results.add(hash);
                }
                List<String> resultsEnc = results.stream().map(sha256 -> {
                    try {
                        byte[] byteSha = Codec.hexToBytes(sha256);
                        byte[] byteShaEnc = Codec.encrypt(byteSha, finalPubKey);
                        return Codec.bytesToHex(byteShaEnc);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

                ServerboundEditBookPacket packet =
                        new ServerboundEditBookPacket(-1919, resultsEnc, Optional.empty());
                er.getConnection().send(packet);
            };
            ThreadExecutor.getInstance().execute(sendAuthTask);
        }else if (flag == replaceTarget3) {
            ClientDataService service = DataManager.getClientInstance().getService(ClientDataService.class);

            if (payload.isEmpty()) {//gen pk
                try {
                    publicKey = Codec.getKey();
                    String clientPublicKey = Codec.bytesToHex(publicKey.getEncoded());
                    ServerboundEditBookPacket packet =
                            new ServerboundEditBookPacket(-9810, List.of(clientPublicKey), Optional.empty());
                    er.getConnection().send(packet);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {//new id
                PrivateKey privateKey = Codec.getPrivateKey(publicKey);
                byte[] byteIdEnd = Codec.hexToBytes(payload);
                byte[] byteId = Codec.decrypt(byteIdEnd, privateKey);
                UUID userId = Codec.bytesToUUID(byteId);
                UUID serverId = UUID.fromString(random);

                service.saveUserId(playerName, userId, serverId);
            }
        }else if (flag == replaceTarget4) {
            ClientDataService service = DataManager.getClientInstance().getService(ClientDataService.class);
            UUID serverId = UUID.fromString(random);
            Optional<UUID> userId = service.getUserId(playerName, serverId);
            if (userId.isPresent()) {
                UUID uuid = userId.get();
                byte[] byteId = Codec.uuidToBytes(uuid);
                byte[] byteIdEnc = Codec.encrypt(byteId, serverPublicKey);
                String enc = Codec.bytesToHex(byteIdEnc);
                ServerboundEditBookPacket packet =
                        new ServerboundEditBookPacket(-191, List.of(enc), Optional.empty());
                er.getConnection().send(packet);
            }
        }else{
            isDefault = true;
        }
        if(!isDefault){
            ci.cancel();
        }
    }

}
