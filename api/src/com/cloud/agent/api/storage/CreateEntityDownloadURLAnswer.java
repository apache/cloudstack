package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;

public class CreateEntityDownloadURLAnswer extends Answer{
    
    String resultString;
    short resultCode;
    public static final short RESULT_SUCCESS = 1;
    public static final short RESULT_FAILURE = 0;
    
    public CreateEntityDownloadURLAnswer(String resultString, short resultCode) {
        super();
        this.resultString = resultString;
        this.resultCode = resultCode;
    }    
    
    public CreateEntityDownloadURLAnswer(){        
    }

}
