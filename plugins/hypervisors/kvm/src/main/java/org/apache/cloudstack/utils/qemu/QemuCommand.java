package org.apache.cloudstack.utils.qemu;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QemuCommand {
    //Qemu agent commands
    public static final String AGENT_FREEZE = "guest-fsfreeze-freeze";
    public static final String AGENT_THAW = "guest-fsfreeze-thaw";
    //Qemu monitor commands
    public static final String QEMU_QUERY_BLOCK_JOBS = "query-block-jobs";
    public static final String QEMU_BLOCK = "query-block";
    public static final String QEMU_DRIVE_BACKUP = "drive-backup";

    public static final String QEMU_CMD = "execute";

    public static Map<String, Object> executeQemuCommand(String command, Map<String, String> args ){
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(QEMU_CMD, command);
        if (args != null) {
            params.put("arguments", args);
        }
        return params;
    }

    public static String getDeviceName(String json, String path) {
        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject) parser.parse(json);
        if (obj.get("return").isJsonArray()) {
            JsonArray arr = obj.get("return").getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).isJsonObject()) {
                    String deviceName = arr.get(i).getAsJsonObject().get("device").getAsString();
                    if (arr.get(i).getAsJsonObject().has("inserted") && arr.get(i).getAsJsonObject().get("inserted").isJsonObject()) {
                        JsonObject inserted = arr.get(i).getAsJsonObject().get("inserted").getAsJsonObject();
                        if (inserted.get("file").getAsString().contains(path)) {
                            return deviceName;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean isJobFinished(String json) {
        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject) parser.parse(json);
        if (obj.get("return").isJsonArray()) {
            JsonArray arr =  obj.get("return").getAsJsonArray();
            if(arr.size() == 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPassed(String result) {
        JsonObject jsonObj = new JsonParser().parse(result).getAsJsonObject();
        return (jsonObj.has("return") && jsonObj.get("return").isJsonObject() && jsonObj.get("return").getAsJsonObject().entrySet().isEmpty());
    }
}
