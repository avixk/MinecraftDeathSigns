package me.avixk.DeathSigns;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlFile {

    final int version = 1;
    File file;
    YamlConfiguration yamlConfiguration = new YamlConfiguration();
    String identifier;

    public YamlFile(String fileString) {
        identifier = fileString;
        this.file = new File(Main.plugin.getDataFolder() + "/" + identifier + ".yml");
        Bukkit.getLogger().info("Registering YamlFile at " + this.file.getAbsolutePath());
        if (!this.file.exists()) {
            Main.plugin.getDataFolder().mkdir();
            try{
                Main.plugin.saveResource(fileString, false);
            }catch (Exception e){}
            try {
                this.file.createNewFile();
            } catch (IOException e) {}
        }
        try {
            yamlConfiguration.load(this.file);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to load " + file.getAbsolutePath() + ", this might be normal if it's the plugin's first time loading.");
        }
        if(!yamlConfiguration.contains("config_version")){
            configVersion = version;
        }else{
            configVersion = yamlConfiguration.getInt("config_version");
        }
    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return yamlConfiguration;
    }

    public int configVersion;
    public int getConfigVersion(){
        return configVersion;
    }

    public void setConfigVersion(int version){
        configVersion = version;
        getConfig().set("config_version", version);
        save();
    }

    int saveTask = 0;
    public void save() {
        if(saveTask != 0){
            Bukkit.getScheduler().cancelTask(saveTask);
            saveTask = 0;
        }
        saveTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(Main.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    yamlConfiguration.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    Bukkit.getLogger().warning("Could not save YamlFile '" + identifier + ".yml'! Data may be lost! It's avixk's fault!");
                }
                saveTask = 0;
            }
        },60);
    }
}

