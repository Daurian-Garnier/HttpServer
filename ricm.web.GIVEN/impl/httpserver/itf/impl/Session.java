package httpserver.itf.impl;

import httpserver.itf.HttpSession;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session implements HttpSession {
    // ConcurrentHashMap because setValue/getValue can be called by many threads
    private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
    private final String my_id;

    public Session() {
        my_id = UUID.randomUUID().toString(); //UUID Unique et safe thread
    }

    @Override
    public String getId() {
        return my_id;
    }

    @Override
    public Object getValue(String key) {
        return data.get(key);
    }

    @Override
    public void setValue(String key, Object value) {
        data.put(key, value);
    }
}
