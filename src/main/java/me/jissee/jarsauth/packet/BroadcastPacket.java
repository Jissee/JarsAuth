package me.jissee.jarsauth.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import javax.annotation.Nullable;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.mojang.blaze3d.Blaze3D.youJustLostTheGame;


public class BroadcastPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final String url;
    private final String hash;
    private final Component prompt;

    public BroadcastPacket(String p_179182_, String p_179183_, @Nullable Component p_179185_){
        this.url = p_179182_;
        this.hash = p_179183_;
        this.prompt = p_179185_;
    }
    public BroadcastPacket(FriendlyByteBuf buf){
        this.url = buf.readUtf();
        this.hash = buf.readUtf(40);
        this.prompt = buf.readNullable(FriendlyByteBuf::readComponent);
    }


    public void encode(FriendlyByteBuf buf){
        buf.writeUtf(this.url);
        buf.writeUtf(this.hash);
        buf.writeNullable(this.prompt, FriendlyByteBuf::writeComponent);
    }

    public static BroadcastPacket decode(FriendlyByteBuf buf){
        return new BroadcastPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{
            if(this.hash.equals("JARSAUTH AUTHENTICATION INFORMATI0N")){//calc and auth
                LOGGER.debug("got packet2 from server");
                Thread thread = new Thread(()->{//todo: client calculate hash
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
                            ctx.get().getNetworkManager().disconnect(Component.translatable("text.disconn.relative"));
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
                    try{
                        PacketHandler.sendToServer(new AuthPacket(-114514, Collections.singletonList(getSMD5.apply(total.append(prompt.getString()).toString()))));
                    }catch(Exception e){
                        LOGGER.error("error when sending auth msg", e);
                    }
                });
                thread.start();
            }else if(this.hash.equals("JARSAUTH AUTHENTICATION INF0RMATION")){//send client archive
                Thread thread = new Thread(()->{
                    Function<File, String> getMD5 = file -> {
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
                            //e.printStackTrace();
                            return "unknown";
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

                    String clientRootDir = Minecraft.getInstance().gameDirectory.getAbsolutePath();
                    if (!clientRootDir.endsWith("\\")) {
                        if(clientRootDir.endsWith(".")){
                            clientRootDir = clientRootDir.substring(0, clientRootDir.length() - 2);
                        }else{
                            clientRootDir = clientRootDir + '/';
                        }
                    }


                    File clientDir = new File(clientRootDir);
                    ArrayList<String> folders = new ArrayList<>();
                    ArrayList<String> files = new ArrayList<>();
                    try{
                        Files.walkFileTree(clientDir.toPath(), new FileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                folders.add(dir.toString());
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                files.add(file.toString());
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
                    }catch (IOException e){
                        LOGGER.error("Error while reading native files: ", e);
                    }

                    ArrayList<String> strl = new ArrayList<>();

                    int i = 0;
                    for(String folder : folders){
                        folder = folder.replace('\\','/');
                        if(i == 100){
                            PacketHandler.sendToServer(new AuthPacket(114514, strl));
                            LOGGER.info("sending info");
                            i = 0;
                            strl.clear();
                        }
                        if(folder.length() > clientRootDir.length()){
                            strl.add(folder.substring(clientRootDir.length()));
                            strl.add("folder");
                            i++;
                        }
                    }
                    for(String file : files){
                        file = file.replace('\\','/');
                        if(i == 100){
                            PacketHandler.sendToServer(new AuthPacket(114514, strl));
                            LOGGER.info("sending info");
                            i = 0;
                            strl.clear();
                        }
                        if(file.length() > clientRootDir.length()){
                            strl.add(file.substring(clientRootDir.length()));
                            strl.add(getMD5.apply(new File(file)));
                            i++;
                        }
                    }
                    strl.add("<end>");
                    strl.add("<end>");
                    PacketHandler.sendToServer(new AuthPacket(114514, strl));
                    LOGGER.info("sending final info");
                });
                thread.start();
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

}
