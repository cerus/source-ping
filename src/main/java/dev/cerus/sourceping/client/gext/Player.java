package dev.cerus.sourceping.client.gext;

import com.google.gson.annotations.SerializedName;

public class Player {

    private final String group;
    private final String nick;
    private final double time;
    @SerializedName("steamid64")
    private final long steamId64;
    private final int score;
    @SerializedName("group_color")
    private final String groupColor;

    public Player(final String group, final String nick, final double time, final long steamId64, final int score, final String groupColor) {
        this.group = group;
        this.nick = nick;
        this.time = time;
        this.steamId64 = steamId64;
        this.score = score;
        this.groupColor = groupColor;
    }

    public String getGroup() {
        return this.group;
    }

    public String getNick() {
        return this.nick;
    }

    public double getTime() {
        return this.time;
    }

    public long getSteamId64() {
        return this.steamId64;
    }

    public int getScore() {
        return this.score;
    }

    public String getGroupColor() {
        return this.groupColor;
    }

}
