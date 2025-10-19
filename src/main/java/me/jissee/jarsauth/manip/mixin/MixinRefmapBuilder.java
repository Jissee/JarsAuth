package me.jissee.jarsauth.manip.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MixinRefmapBuilder {
    private final Map<String, JsonElement> mappingsBuffer = new HashMap<>();
    private final Map<String, JsonElement> data$seargeBuffer = new HashMap<>();

    private final Map<String, JsonElement> mappingsBufferObf = new HashMap<>();
    private final Map<String, JsonElement> data$seargeBufferObf = new HashMap<>();

    public MixinRefmapBuilder(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // 取 mappings
        JsonObject mappings = root.getAsJsonObject("mappings");


        //System.out.println("=== mappings 部分 ===");
        for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
            mappingsBuffer.put(entry.getKey(), entry.getValue());
        }

        // 取 data.searge
        JsonObject data = root.getAsJsonObject("data");
        if (data != null && data.has("searge")) {
            JsonObject searge = data.getAsJsonObject("searge");
            //System.out.println("=== data.searge 部分 ===");
            for (Map.Entry<String, JsonElement> entry : searge.entrySet()) {
                data$seargeBuffer.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addObf(String originalName, String obfName) {
        JsonElement mappingsElement = mappingsBuffer.get(originalName);
        if (mappingsElement != null) {
            mappingsBufferObf.put(obfName, mappingsElement);
        }

        JsonElement data$seargeElement = data$seargeBuffer.get(originalName);
        if (data$seargeElement != null) {
            data$seargeBufferObf.put(obfName, data$seargeElement);
        }

    }

    public String toStringObf() {
        JsonObject root = new JsonObject();
        JsonObject mappings = new JsonObject();
        mappingsBufferObf.forEach(mappings::add);
        JsonObject data = new JsonObject();
        JsonObject searge = new JsonObject();
        data$seargeBufferObf.forEach(searge::add);
        root.add("mappings", mappings);
        data.add("searge", searge);
        root.add("data", data);
        return root.toString();
    }

}
