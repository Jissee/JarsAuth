package me.jissee.jarsauth.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class ClientProfile {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final ArrayList<String> comments;
    private final int interval;
    private final int timeout;
    private final ArrayList<String> inclusion;
    private final ArrayList<String> refuseMessage;
    private ClientProfile(
            ArrayList<String> comments,
            int interval,
            int timeout,
            ArrayList<String> inclusion,
            ArrayList<String> refuseMessage
    ) {
        this.comments = comments;
        this.interval = interval;
        this.timeout = timeout;
        this.inclusion = inclusion;
        this.refuseMessage = refuseMessage;
    }
    public static ClientProfile load(String serverSaveDir){
        File serverProfileFile = new File( serverSaveDir + File.separator + "jarsauth-profile.json");
        try {
            if(!serverProfileFile.exists()){
                LOGGER.info("Cannot find " + serverProfileFile + ", generating default profile.");

                serverProfileFile.createNewFile();
                FileWriter fw = new FileWriter(serverProfileFile);
                ClientProfile profile = createDefaultProfile();
                fw.write(gson.toJson(profile));
                fw.close();
                return profile;
            }

            FileReader fr = new FileReader(serverProfileFile);
            int ch;
            StringBuilder sb = new StringBuilder();
            while((ch = fr.read()) != -1){
                sb.append((char)ch);
            }
            String str = sb.toString();
            fr.close();
            return gson.fromJson(str, ClientProfile.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static ClientProfile createDefaultProfile() {
        int interval = 20;
        int timeout = 10;
        ArrayList<String> comments = new ArrayList<>();
        ArrayList<String> inclusion = new ArrayList<>();
        ArrayList<String> refuseMessage = new ArrayList<>();

        comments.add("如果你清楚配置的规则，你可以删除这里的注释");
        comments.add("星号结尾表示文件夹中所有文件及子文件夹中的所有文件");
        comments.add("以特定的名称结尾，表示某个文件或者某个文件夹，这里的文件夹只检查子文件，不包括子文件夹中的文件");
        comments.add("所有在inclusion中的文件或文件夹将会被检查，");
        comments.add("在默认配置中，mods文件夹下所有文件（包括子文件夹中的文件）");
        comments.add("resourcepacks下面的examplefile.zip单个文件，不包含此文件夹中其他的文件");
        comments.add("shaderpacks文件夹中所有的文件，但不包含shaderpacks中任何子文件夹中的文件");
        comments.add("以及.minecraft（根目录）下面的example.txt文件会被检查");
        comments.add("<refuseMessage>是客户端被拒绝时显示的信息");
        comments.add("<interval>服务器在运行中将每间隔多少秒进行一次检测");
        comments.add("<timeout>如果客户端没能返回验证数据，将在多少秒后拒绝连接");

        comments.add("If you are clear about the config rule, you can remove the comments");
        comments.add("The paths end with * represent folders and all files and subfolders in it.");
        comments.add("The paths end with a specific name represent specific files or only subfiles in specific folders");
        comments.add("All files or folders in <inclusion> will be checked.");
        comments.add("In the default config, all files and subfolders in <mods> will be checked,");
        comments.add("a file called <examplefile.zip> in <resourcepacks> will be checked,");
        comments.add("all files in <shaderpacks> but not in subfolders will be checked,");
        comments.add("a file called <example.txt> in <.minecraft> will be checked.");
        comments.add("<refuseMessage> is the information display on client when it is refused to connect.");
        comments.add("<interval> indicates every how many seconds will the server tries to check all clients.");
        comments.add("<timeout> indicates after how many seconds a client will be refused if it cannot return auth result.");


        inclusion.add("mods/*");
        inclusion.add("resourcepacks/examplefile.zip");
        inclusion.add("shaderpacks");
        inclusion.add("example.txt");

        refuseMessage.add("你的客户端签名和服务端记录不一致");
        refuseMessage.add("Your client signature does not match server records");

        return new ClientProfile(comments, interval, timeout, inclusion, refuseMessage);
    }

    public ArrayList<String> getRefuseMessage() {
        return refuseMessage;
    }

    public long getInterval() {
        return interval;
    }
    public long getTimeout(){
        return timeout;
    }
    public ArrayList<String> getInclusion(){
        return inclusion;
    }
}