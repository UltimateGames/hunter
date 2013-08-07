package me.ampayne2.Hunter;

import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.json.JSONArray;
import me.ampayne2.ultimategames.json.JSONException;
import me.ampayne2.ultimategames.json.JSONObject;
import me.ampayne2.ultimategames.webapi.WebHandler;

public class WebKillHandler implements WebHandler{

    private Arena arena;
    private Hunter hunter;
    public WebKillHandler(Hunter hunter, Arena arena) {
        this.arena = arena;
        this.hunter = hunter;
    }
    @Override
    public String sendResult() {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.append("civilians", hunter.getCivilians(arena));
            jsonObject.append("hunters", hunter.getHunters(arena));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonArray.put(jsonObject);
        return jsonArray.toString();
    }
}
