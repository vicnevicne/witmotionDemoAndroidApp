package com.bt901.dialog;

public interface IEvent {

    /**
     * event type
     *
     * @return
     */
    String getEventType();

    void setEventType(String eventType);

    /**
     * event data
     *
     * @return
     */
    Object getData();

    void setData(Object data);
}
