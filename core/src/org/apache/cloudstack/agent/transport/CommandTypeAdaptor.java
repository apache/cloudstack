package org.apache.cloudstack.agent.transport;

import java.io.IOException;

import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class CommandTypeAdaptor extends TypeAdapter<Command> {
    protected Gson _gson = null;

    public void initGson(Gson gson) {
        _gson = gson;
    }

    @Override
    public void write(JsonWriter out, Command cmd) throws IOException {
        // TODO Auto-generated method stub
        String data = _gson.toJson(cmd);
        out.name(cmd.getClass().getCanonicalName());
        if(data != null && !data.equals("null")) {
            out.jsonValue(data);
        } else {
            out.nullValue();
        }
    }

    @Override
    public Command read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        in.beginObject();
        String tiep = in.nextName();
        Command cmd;
        try {
            Class<Command> clz = (Class<Command>)Class.forName(tiep);
            cmd = _gson.fromJson(in, clz);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException("deserializing json failed, couldn't load " + tiep, e);
        }
        in.endObject();
        return cmd;
    }

}
