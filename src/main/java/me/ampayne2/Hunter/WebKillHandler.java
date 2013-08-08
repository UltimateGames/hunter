package me.ampayne2.Hunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.gson.Gson;
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
        Gson gson = new Gson();
        Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        map.put("civilians", hunter.getCivilians(arena));
        map.put("hunters", hunter.getHunters(arena));
        return gson.toJson(map);
    }
}
