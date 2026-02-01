package me.jissee.jarsauth.manip.mixin;

import com.google.gson.*;

import java.util.*;

public class MixinConfigBuilder {
    private final JsonObject root;
    private final JsonArray mixins;
    private final JsonArray client;

    public MixinConfigBuilder(String json) {
        root = JsonParser.parseString(json).getAsJsonObject();

        if (root.has("mixins") && root.get("mixins").isJsonArray()) {
            mixins = root.getAsJsonArray("mixins");
        } else {
            mixins = new JsonArray();
            root.add("mixins", mixins);
        }
        if (root.has("client") && root.get("client").isJsonArray()) {
            client = root.getAsJsonArray("client");
        } else {
            client = new JsonArray();
            root.add("client", client);
        }
    }

    public void addCommon(String className) {
        for (JsonElement element : mixins) {
            if (element.getAsString().equals(className)) {
                return;
            }
        }
        mixins.add(className);
    }

    public void addCommonAll(Collection<String> classNames) {
        if (classNames == null || classNames.isEmpty()) {
            return;
        }

        Set<String> existing = new HashSet<>();
        for (JsonElement element : mixins) {
            existing.add(element.getAsString());
        }

        for (String className : classNames) {
            if (existing.add(className)) {
                mixins.add(className);
            }
        }
    }


    public List<String> getCommonMixinClasses() {
        List<String> mixinNames = new ArrayList<>();
        for (JsonElement element : mixins) {
            mixinNames.add(element.getAsString());
        }
        return mixinNames;
    }

    public void removeCommon(String className) {
        List<JsonElement> toRemove = new ArrayList<>();
        for (JsonElement element : mixins) {
            if (element.getAsString().equals(className)) {
                toRemove.add(element);
            }
        }
        toRemove.forEach(mixins::remove);
    }

    /** 一次性清空所有 mixins */
    public void removeAllCommon() {
        while (!mixins.isEmpty()) {
            mixins.remove(0);
        }
    }

    public void addClient(String className) {
        for (JsonElement element : client) {
            if (element.getAsString().equals(className)) {
                return;
            }
        }
        client.add(className);
    }

    public void addClientAll(Collection<String> classNames) {
        if (classNames == null || classNames.isEmpty()) {
            return;
        }

        Set<String> existing = new HashSet<>();
        for (JsonElement element : client) {
            existing.add(element.getAsString());
        }

        for (String className : classNames) {
            if (existing.add(className)) {
                client.add(className);
            }
        }
    }


    public List<String> getClientMixinClasses() {
        List<String> mixinNames = new ArrayList<>();
        for (JsonElement element : client) {
            mixinNames.add(element.getAsString());
        }
        return mixinNames;
    }

    public void removeClient(String className) {
        List<JsonElement> toRemove = new ArrayList<>();
        for (JsonElement element : client) {
            if (element.getAsString().equals(className)) {
                toRemove.add(element);
            }
        }
        toRemove.forEach(client::remove);
    }

    /** 一次性清空所有 mixins */
    public void removeAllClient() {
        while (!client.isEmpty()) {
            client.remove(0);
        }
    }

    @Override
    public String toString() {
        // 先将 mixins 中的字符串取出并排序
        List<String> sortedMixins = new ArrayList<>();
        for (JsonElement element : mixins) {
            sortedMixins.add(element.getAsString());
        }
        Collections.sort(sortedMixins);

        // 清空并按排序后的顺序重新写入
        removeAllCommon();
        for (String mixin : sortedMixins) {
            mixins.add(mixin);
        }

        sortedMixins.clear();
        for (JsonElement element : client) {
            sortedMixins.add(element.getAsString());
        }
        Collections.sort(sortedMixins);

        // 清空并按排序后的顺序重新写入
        removeAllClient();
        for (String mixin : sortedMixins) {
            client.add(mixin);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(root);
    }

}
