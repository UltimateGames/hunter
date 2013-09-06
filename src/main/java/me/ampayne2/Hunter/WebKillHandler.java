package me.ampayne2.hunter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.gson.Gson;
import me.ampayne2.ultimategames.webapi.WebHandler;

public class WebKillHandler implements WebHandler{

    private UltimateGames ug;
    private Arena arena;
    
    public WebKillHandler(UltimateGames ultimateGames, Arena arena) {
        this.ug = ultimateGames;
        this.arena = arena;
    }
    @Override
    public String sendResult() {
        Gson gson = new Gson();
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("civilians", ug.getTeamManager().getTeam(arena, "Civilian").getPlayers());
        map.put("hunters", ug.getTeamManager().getTeam(arena, "Hunter").getPlayers());
        return gson.toJson(map);
    }
}
