package me.ampayne2.hunter;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.webapi.WebHandler;
import me.ampayne2.ultimategames.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HunterWebHandler implements WebHandler {
    private UltimateGames ug;
    private Arena arena;

    public HunterWebHandler(UltimateGames ultimateGames, Arena arena) {
        this.ug = ultimateGames;
        this.arena = arena;
    }

    @Override
    public String sendResult() {
        Gson gson = new Gson();
        Map<String, List<String>> map = new HashMap<>();
        map.put("civilians", ug.getTeamManager().getTeam(arena, "Civilian").getPlayers());
        map.put("hunters", ug.getTeamManager().getTeam(arena, "hunter").getPlayers());
        return gson.toJson(map);
    }
}
