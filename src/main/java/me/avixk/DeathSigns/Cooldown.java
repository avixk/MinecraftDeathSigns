package me.avixk.DeathSigns;

import java.util.HashMap;

public class Cooldown {
    static HashMap<String,Cooldown> cooldowns = new HashMap<>();
    long timeStarted;
    String name;
    long length_millis;
    public Cooldown(String name, int length_millis){
        this.name = name;
        this.timeStarted = 0;
        this.length_millis = length_millis;
    }
    public void start(){
        timeStarted = System.currentTimeMillis();
        if(!cooldowns.containsKey(name))cooldowns.put(name,this);
    }
    public String getName(){
        return name;
    }
    public long getTimeStarted(){
        return timeStarted;
    }
    public long getLength(){
        return length_millis;
    }
    public long getTimeRemaining(){
        return timeStarted + length_millis - System.currentTimeMillis();
    }
    public static long getTimeRemaining(String cooldown){
        if(cooldowns.containsKey(cooldown)){
            long remaining = cooldowns.get(cooldown).getTimeRemaining();
            if(remaining <= 0){
                cooldowns.remove(cooldown);
            }
            return remaining;
        }
        return 0;
    }
}

