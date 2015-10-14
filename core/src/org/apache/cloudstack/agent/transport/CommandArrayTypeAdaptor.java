package org.apache.cloudstack.agent.transport;

import java.lang.reflect.Array;

import com.cloud.agent.api.Command;
import com.google.gson.stream.JsonWriter;

public class CommandArrayTypeAdaptor extends GenericArrayTypeAdaptor<Command> {
    protected Command[] newArray(int size) {
        Command[] commands = (Command[])Array.newInstance(Command.class, size);
        return commands;
    }
    @Override
    protected void writeElement(JsonWriter out, Command elem) {
        _gson.toJson(elem,elem.getClass(),out);
    }

}
