package me.jissee.jarsauth.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.mojang.blaze3d.Blaze3D.youJustLostTheGame;

@Mixin(value = ClientboundResourcePackPacket.class, priority = 0)
public class ClientboundPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Shadow @Final
    private String url; // auth profile

    @Shadow @Final
    private String hash; // auth flag

    @Shadow @Final
    private Component prompt; // hash salt

    @Inject(method = {"handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void inj1(ClientGamePacketListener p_132923_, CallbackInfo ci){
        if(this.hash.equals("JARSAUTH AUTHENTICATION INFORMATI0N")){//calc and auth
            LOGGER.debug("got packet1 from server");
            Thread thread = new Thread(()->{//todo: client calculate hash
                ClientPacketListener er = (ClientPacketListener) p_132923_;

                Function<File, String> getFMD5 = (file) -> {
                    FileInputStream fileInputStream = null;
                    try {
                        MessageDigest MD5 = MessageDigest.getInstance("MD5");
                        fileInputStream = new FileInputStream(file);
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = fileInputStream.read(buffer)) != -1) {
                            MD5.update(buffer, 0, length);
                        }
                        return new String(Hex.encodeHex(MD5.digest()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        try {
                            if (fileInputStream != null){
                                fileInputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Function<String, String> getSMD5 = (str) -> {
                    if (str == null || str.length() == 0) {
                        throw new IllegalArgumentException("String to encript cannot be null or zero length");
                    }
                    StringBuffer hexString = new StringBuffer();
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        md.update(str.getBytes());
                        byte[] hash = md.digest();
                        for (int i = 0; i < hash.length; i++) {
                            if ((0xff & hash[i]) < 0x10) {
                                hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                            } else {
                                hexString.append(Integer.toHexString(0xFF & hash[i]));
                            }
                        }
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    return hexString.toString();
                };
                Predicate<String> checkRelativePath = (path) -> path.contains("..");

                ArrayList<String> rawIncl = (ArrayList<String>) gson.fromJson(url, TypeToken.getParameterized(ArrayList.class, String.class));
                ArrayList<String> procIncl = new ArrayList<>();
                ArrayList<String> procType = new ArrayList<>();
                ArrayList<String> folders = new ArrayList<>();
                ArrayList<String> files = new ArrayList<>();

                for(String incl : rawIncl){
                    incl = incl.replace('\\', '/');
                    if(checkRelativePath.test(incl)){
                        er.onDisconnect(Component.translatable("text.disconn.relative"));
                        return;
                    }
                    int lastpos = incl.lastIndexOf('/');
                    String former;
                    String latter;
                    if(lastpos > -1){
                        former = incl.substring(0, lastpos);
                        latter = incl.substring(lastpos + 1);
                    }else{
                        former = "";
                        latter = incl;
                    }

                    if(latter.equals("*")){
                        procIncl.add(former);
                        procType.add("*");
                    }else{
                        procIncl.add(incl);
                        procType.add("");
                    }

                }
                StringBuilder total = new StringBuilder();
                String clientRootDir = Minecraft.getInstance().gameDirectory.getAbsolutePath();
                if (!clientRootDir.endsWith("\\")) {
                    if(clientRootDir.endsWith(".")){
                        clientRootDir = clientRootDir.substring(0, clientRootDir.length() - 2);
                    }else{
                        clientRootDir = clientRootDir + '/';
                    }
                }

                for(int i = 0; i < procIncl.size(); i++){
                    String incl = procIncl.get(i);
                    String type = procType.get(i);
                    Path startPath = Path.of(clientRootDir + incl);
                    if(type.equals("*")){
                        try {
                            String finalClientRootDir = clientRootDir;
                            Files.walkFileTree(startPath, new FileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                    String s = dir.toString().replace('\\','/');
                                    try{
                                        s = s.substring(finalClientRootDir.length());
                                        folders.add(s);
                                    }catch(IndexOutOfBoundsException e){}
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    String s = file.toString();
                                    try{
                                        s = s.substring(finalClientRootDir.length()).replace('\\','/');
                                        files.add(s);
                                    }catch(IndexOutOfBoundsException e){}
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }else{
                        if(Files.isDirectory(startPath)){
                            folders.add(incl);
                            try(Stream<Path> stream = Files.list(startPath)) {
                                String finalClientRootDir1 = clientRootDir;
                                stream.forEach((path) -> {
                                    if(!Files.isDirectory(path)){
                                        files.add(path.toString().substring(finalClientRootDir1.length()).replace('\\', '/'));
                                    }
                                });
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }else{
                            files.add(incl);
                        }
                    }
                }
                folders.sort(String::compareTo);
                files.sort(String::compareTo);
                for(String str : folders){
                    File folder = new File(clientRootDir + str);
                    if(folder.exists()){
                        total.append(str).append('/').append("folder").append('\n');
                    }
                }
                for(String str : files){
                    File file = new File(clientRootDir + str);
                    if(file.exists()){
                        total.append(str).append(getFMD5.apply(file)).append('\n');
                    }
                }
                LOGGER.debug(total.toString());
                try{
                    er.send(new ServerboundEditBookPacket(-114514, Collections.singletonList(getSMD5.apply(total.append(prompt.getString()).toString())), Optional.of("null")));

                }catch(Exception e){
                    LOGGER.error("error when sending auth msg", e);
                }
            });
            thread.start();
            ci.cancel();
        }
    }
}
