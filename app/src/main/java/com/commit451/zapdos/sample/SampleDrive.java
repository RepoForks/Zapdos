package com.commit451.zapdos.sample;

import com.commit451.zapdos.drive.Body;
import com.commit451.zapdos.drive.CREATE;
import com.commit451.zapdos.drive.Path;
import com.commit451.zapdos.drive.READ;

import rx.Observable;

/**
 * Sample drive interface
 */
public interface SampleDrive {

    @READ("message/{message_file_name}")
    Observable<Message> getMessage(@Path("message_file_name") String messageFileName);

    @CREATE("message/{message_id}")
    Observable<Message> writeMessage(@Path("message_id") String messageFileName, @Body Message message);

}
