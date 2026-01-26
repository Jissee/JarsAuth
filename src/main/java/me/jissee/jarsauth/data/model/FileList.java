package me.jissee.jarsauth.data.model;

import me.jissee.jarsauth.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record FileList(List<String> folders, List<String> files) {
    private static final Logger LOGGER = LoggerFactory.getLogger("Hash Info");
    public String hash(AcceptedDetail detail, String random) {
        Map<String, String> filesInDetail = detail.files();
        Set<String> foldersInDetail = new HashSet<>(detail.folders());
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(random).append(">").append("\n");
        for (String folder : folders) {
            sb.append(folder).append(" -> ");
            if (foldersInDetail.contains(folder)) {
                sb.append("FOLDER_EXIST");
            }else{
                sb.append("N0_FOLDER_EXIST");
            }
            sb.append("\n");
        }
        for (String file : files) {
            String hash = filesInDetail.get(file);
            if (hash == null) {
                hash = "N0_FILE_EXIST";
            }
            sb.append(file).append(" -> ").append(hash).append("\n");
        }
        LOGGER.debug("Server Original:\n{}", sb.toString());
        return Codec.getSSHA256(sb.toString());
    }
    public String hash(String root, String random){
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(random).append(">").append("\n");
        for (String folder : folders) {
            sb.append(folder).append(" -> ");
            if (Files.exists(Path.of(root + folder))) {
                sb.append("FOLDER_EXIST");
            }else{
                sb.append("N0_FOLDER_EXIST");
            }
            sb.append("\n");
        }
        for (String file : files) {
            String hash;
            File fileIn = Path.of(root + file).toFile();
            if (fileIn.exists() && fileIn.isFile()) {
                hash = Codec.getFSHA256(new File(root + file));
            } else {
                hash = "N0_FILE_EXIST";
            }
            sb.append(file).append(" -> ").append(hash).append("\n");
        }
        LOGGER.debug("Client Original:\n{}", sb.toString());
        return Codec.getSSHA256(sb.toString());
    }
}
