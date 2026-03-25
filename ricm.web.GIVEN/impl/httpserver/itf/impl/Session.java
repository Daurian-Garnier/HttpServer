package httpserver.itf.impl;

import httpserver.itf.HttpSession;

import java.util.HashMap;

public class Session implements HttpSession {

    private final HashMap<String, Object> data = new HashMap<>();
    private final String my_id;
    static int id = 0;

    public Session(){
        my_id = String.valueOf(id++);
    }

    @Override
    public String getId() {
        return my_id;
    }

    @Override
    public Object getValue(String key) {
        return data.getOrDefault(key, null);
    }

    @Override
    public void setValue(String key, Object value) {
        data.put(key, value);
    }
}
