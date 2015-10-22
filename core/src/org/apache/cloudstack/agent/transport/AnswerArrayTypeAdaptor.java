package org.apache.cloudstack.agent.transport;

import java.io.IOException;
import java.lang.reflect.Array;

import com.cloud.agent.api.Answer;
import com.cloud.serializer.GsonHelper;
import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

public class AnswerArrayTypeAdaptor extends GenericArrayTypeAdaptor<Answer> {

    @Override
    protected Answer[] newArray(int size) {
        Answer[] answers = (Answer[])Array.newInstance(Answer.class, size);
        return answers;
    }
    public void initGson(Gson gson) {
        _gson = gson;
        _adaptor = _gson.getAdapter(Answer.class);
    }
    TypeAdapter<Answer> _adaptor;

    @Override
    protected void writeElement(JsonWriter out, Answer elem) throws IOException {
        // get the gsonHelper.
        ExclusionStrategy excluder = GsonHelper.getExcluder();
        if (!excluder.shouldSkipClass(elem.getClass())) {
            out.beginObject();
            _adaptor.write(out,elem);
            out.endObject();
        }
    }

}
