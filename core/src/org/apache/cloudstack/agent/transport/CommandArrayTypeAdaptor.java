package org.apache.cloudstack.agent.transport;

import java.io.IOException;
import java.lang.reflect.Array;

import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.stream.JsonWriter;

public class CommandArrayTypeAdaptor extends GenericArrayTypeAdaptor<Command> {
    protected Command[] newArray(int size) {
        Command[] commands = (Command[])Array.newInstance(Command.class, size);
        return commands;
    }
    @Override
    protected void writeElement(JsonWriter out, Command elem) {
        try {
            String data = _gson.toJson(elem);
            if(data != null && !data.equals("null")) {
                out.name(elem.getClass().getCanonicalName());
                out.jsonValue(data);
            }
        } catch (IOException e) {
            throw new CloudRuntimeException("serializing json failed for " + elem.getClass(), e);
        }
    }

}
