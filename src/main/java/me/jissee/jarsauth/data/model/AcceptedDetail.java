package me.jissee.jarsauth.data.model;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public record AcceptedDetail(
        String groupName,
        Map<String, String> files,
        List<String> folders
) {

    public AcceptedDetail(String groupName) {
        this(groupName, new TreeMap<>(), new ArrayList<>());
    }

    public void addFile(String key, String value){
        if(key.startsWith("/")) key = key.substring(1);
        if(key.endsWith("/")) key = key.substring(0, key.length() - 1);
        files.put(key, value);
    }

    public void addFolder(String key){
        if(key.startsWith("/")) key = key.substring(1);
        if(key.endsWith("/")) key = key.substring(0, key.length() - 1);
        folders.add(key);
    }

    public void addAll(AcceptedDetail detail){
        files.putAll(detail.files);
        folders.addAll(detail.folders);
    }

    public int getTotalCount(){
        return folders.size() + files.size();
    }
}
