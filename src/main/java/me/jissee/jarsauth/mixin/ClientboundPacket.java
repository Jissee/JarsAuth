/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.Codec;
import me.jissee.jarsauth.FileCollector;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


@Mixin(value = ClientboundResourcePackPacket.class, priority = 0)
public class ClientboundPacket {
    private static final int replaceTarget1 = -101;
    private static final int replaceTarget2 = 114514;
    private static final int replaceTarget3 = -109;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientboundPacket.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Shadow @Final
    private String url; // auth profile (ArrayList<String> raw incl)

    @Shadow @Final
    private String hash; // auth flag (auth or record)

    @Shadow @Final
    private Component prompt; // hash salt

    @Inject(method = {"handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void inj1(ClientGamePacketListener p_132923_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(this.hash.equals("JARSAUTH AUTHENTICATION INFORMATI0N")) {//calc and auth

        }else if(this.hash.equals("JARSAUTH AUTHENTICATION INF0RMATION")) {//send client archive
            ClientPacketListener er = (ClientPacketListener) p_132923_;

            Thread thread = new Thread(()->{
                File defaultFile = new File("");
                defaultFile = defaultFile.getAbsoluteFile();

                String clientRootDir = defaultFile.getAbsolutePath();
                if (!clientRootDir.endsWith(File.separator)) {
                    if(clientRootDir.endsWith(".")){
                        clientRootDir = clientRootDir.substring(0, clientRootDir.length() - 2);
                    }else{
                        clientRootDir = clientRootDir + File.separator;
                    }
                }


                File clientDir = new File(clientRootDir);
                List<String> folders = new LinkedList<>();
                List<String> files   = new LinkedList<>();
                try{
                    Files.walkFileTree(clientDir.toPath(), new FileCollector(folders, files));
                }catch (IOException e){
                    LOGGER.error("Error while reading native files: ", e);
                }

                List<String> strl = new LinkedList<>();

                int i = 0;
                int totalCount = files.size() + folders.size();
                int packetsSent = 0;
                for(String folder : folders){
                    folder = folder.replace('\\','/');
                    if(i == 100){
                        ServerboundEditBookPacket packet = new ServerboundEditBookPacket(replaceTarget2, strl, Optional.of(String.valueOf(totalCount)));
                        er.send(packet);
                        packetsSent++;
                        LOGGER.info("sending info [{}/{}]", packetsSent, totalCount / 100 + 1);
                        i = 0;
                        strl.clear();
                    }
                    if(folder.length() >= clientRootDir.length()){
                        String key = folder.substring(clientRootDir.length());
                        String value = "folder";
                        strl.add(key);
                        strl.add(value);
                        i++;
                    }else{
                        totalCount--;
                        LOGGER.warn("Cannot access folder {}", folder);
                    }
                }
                for(String file : files){
                    file = file.replace('\\','/');
                    if(i == 100){
                        ServerboundEditBookPacket packet = new ServerboundEditBookPacket(replaceTarget2, strl, Optional.of(String.valueOf(totalCount)));
                        er.send(packet);
                        packetsSent++;
                        LOGGER.info("sending info [{}/{}]", packetsSent, totalCount / 100 + 1);
                        i = 0;
                        strl.clear();
                    }
                    if(file.length() >= clientRootDir.length()){
                        String key = file.substring(clientRootDir.length());
                        String value = Codec.getFSHA256(new File(file));
                        strl.add(key);
                        strl.add(value);
                        i++;
                    }else{
                        totalCount--;
                        LOGGER.warn("Cannot access file {}", file);
                    }
                }
                if(!strl.isEmpty()){
                    ServerboundEditBookPacket packet = new ServerboundEditBookPacket(replaceTarget2, strl, Optional.of(String.valueOf(totalCount)));
                    er.send(packet);
                    packetsSent++;
                    LOGGER.info("sending info [{}/{}]", packetsSent, totalCount / 100 + 1);
                }
            });
            thread.start();
            ci.cancel();
        }
    }
}

