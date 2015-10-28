package org.apache.cloudstack.agent.transport;

import java.io.IOException;
import java.lang.reflect.Array;

import com.cloud.agent.api.Command;
import com.cloud.serializer.GsonHelper;
import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

public class CommandArrayTypeAdaptor extends GenericArrayTypeAdaptor<Command> {

    @Override
    protected Command[] newArray(int size) {
        Command[] commands = (Command[])Array.newInstance(Command.class, size);
        return commands;
    }
    public void initGson(Gson gson) {
        _gson = gson;
        _adaptor = _gson.getAdapter(Command.class);
    }
    TypeAdapter<Command> _adaptor;

    @Override
    protected void writeElement(JsonWriter out, Command elem) throws IOException {
        // get the gsonHelper.
        ExclusionStrategy excluder = GsonHelper.getExcluder();
        if (!excluder.shouldSkipClass(elem.getClass())) {
            out.beginObject();
            _adaptor.write(out,elem);
            out.endObject();
        }
    }

}
