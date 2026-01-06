package me.jissee.jarsauth.manip.mixin;

import com.google.gson.*;

import java.util.*;

public class MixinConfigBuilder {
    private final JsonObject root;
    private final JsonArray mixins;

    public MixinConfigBuilder(String json) {
        root = JsonParser.parseString(json).getAsJsonObject();

        if (root.has("mixins") && root.get("mixins").isJsonArray()) {
            mixins = root.getAsJsonArray("mixins");
        } else {
            mixins = new JsonArray();
            root.add("mixins", mixins);
        }
    }

    public void add(String className) {
        for (JsonElement element : mixins) {
            if (element.getAsString().equals(className)) {
                return;
            }
        }
        mixins.add(className);
    }

    public void addAll(Collection<String> classNames) {
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


    public List<String> getMixinClasses() {
        List<String> mixinNames = new ArrayList<>();
        for (JsonElement element : mixins) {
            mixinNames.add(element.getAsString());
        }
        return mixinNames;
    }

    public void remove(String className) {
        List<JsonElement> toRemove = new ArrayList<>();
        for (JsonElement element : mixins) {
            if (element.getAsString().equals(className)) {
                toRemove.add(element);
            }
        }
        toRemove.forEach(mixins::remove);
    }

    /** 一次性清空所有 mixins */
    public void removeAll() {
        while (!mixins.isEmpty()) {
            mixins.remove(0);
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
        removeAll();
        for (String mixin : sortedMixins) {
            mixins.add(mixin);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(root);
    }

}
