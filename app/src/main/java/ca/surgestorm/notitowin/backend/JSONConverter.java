package ca.surgestorm.notitowin.backend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JSONConverter extends JSONObject {
    private long id;
    private JSONObject mainBody;

    public JSONConverter() {
        this.id = System.currentTimeMillis();
        mainBody = new JSONObject();
    }

    public JSONObject getMainBody() {
        return mainBody;
    }

    public long getId() {
        return id;
    }

    //================================================================================
    // Set Methods
    //================================================================================

    public void set(String key, String value) {
        if (value == null) return;
        try {
            mainBody.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, int value) {
        try {
            mainBody.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, boolean value) {
        try {
            mainBody.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, double value) {
        try {
            mainBody.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, JSONArray value) {
        try {
            mainBody.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, JSONObject value) {
        try {
            mainBody.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, Set<String> value) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String s : value) {
                jsonArray.put(s);
            }
            mainBody.put(key, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, List<String> value) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String s : value) {
                jsonArray.put(s);
            }
            mainBody.put(key, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //================================================================================
    // Get Methods
    //================================================================================

    public String getString(String key) {
        return mainBody.optString(key, "");
    }

    public String getString(String key, String defaultValue) {
        return mainBody.optString(key, defaultValue);
    }

    public int getInt(String key) {
        return mainBody.optInt(key, -1);
    }

    public int getInt(String key, int defaultValue) {
        return mainBody.optInt(key, defaultValue);
    }

    public long getLong(String key) {
        return mainBody.optLong(key, -1);
    }

    public long getLong(String key, long defaultValue) {
        return mainBody.optLong(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return mainBody.optBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return mainBody.optBoolean(key, defaultValue);
    }

    public double getDouble(String key) {
        return mainBody.optDouble(key, Double.NaN);
    }

    public double getDouble(String key, double defaultValue) {
        return mainBody.optDouble(key, defaultValue);
    }

    public JSONArray getJSONArray(String key) {
        return mainBody.optJSONArray(key);
    }

    public JSONObject getJSONObject(String key) {
        return mainBody.optJSONObject(key);
    }

    private Set<String> getStringSet(String key) {
        JSONArray jsonArray = mainBody.optJSONArray(key);
        if (jsonArray == null) return null;
        Set<String> list = new HashSet<>();
        int length = jsonArray.length();
        for (int i = 0; i < length; i++) {
            try {
                String str = jsonArray.getString(i);
                list.add(str);
            } catch (Exception e) {
            }
        }
        return list;
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        if (mainBody.has(key)) return getStringSet(key);
        else return defaultValue;
    }

    public List<String> getStringList(String key) {
        JSONArray jsonArray = mainBody.optJSONArray(key);
        if (jsonArray == null) return null;
        List<String> list = new ArrayList<>();
        int length = jsonArray.length();
        for (int i = 0; i < length; i++) {
            try {
                String str = jsonArray.getString(i);
                list.add(str);
            } catch (Exception e) {
            }
        }
        return list;
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        if (mainBody.has(key)) return getStringList(key);
        else return defaultValue;
    }

    //================================================================================
    // Other Methods
    //================================================================================

    public boolean has(String key) {
        return mainBody.has(key);
    }


    public String serialize() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("id", id);
        jo.put("body", mainBody);
        //QJSon does not escape slashes, but Java JSONObject does. Converting to QJson format.
        return jo.toString().replace("\\/", "/") + "\n";
    }
}
