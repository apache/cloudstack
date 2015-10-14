package org.apache.cloudstack.agent.transport;

import java.io.IOException;
import java.util.ArrayList;

import com.cloud.serializer.GsonHelper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public abstract class GenericArrayTypeAdaptor<T> extends TypeAdapter<T[]> {
    protected Gson _gson = null;

    public GenericArrayTypeAdaptor() {
    }

    public void initGson(Gson gson) {
        _gson = gson;
    }


    @Override
    public void write(JsonWriter out, T[] value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginArray();
        for (T elem : (T[])value) {
            out.beginObject();
            writeElement(out, elem);
            out.endObject();
        }
        out.endArray();
    }

    @Override
    public T[] read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        ArrayList<T> arry = new ArrayList<T>();
        in.beginArray();
        while (in.hasNext()) {
            in.beginObject();
            String tiep = in.nextName();
            try {
                Class<T> clz = (Class<T>)Class.forName(tiep);
                T t = _gson.fromJson(in, clz);
                arry.add(t);
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException("desiralizing json failed, couldn't load " + tiep, e);
            }
            in.endObject();
        }
        in.endArray();
        T[] result = newArray(arry.size());
        result = arry.toArray(result);
        return result;
    }
    abstract protected T[] newArray(int size);
    abstract protected void writeElement(JsonWriter out, T elem);
}
