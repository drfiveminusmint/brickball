package com.github.drfiveminusmint.brickball;

import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.command.BrickballCommand;
import com.github.drfiveminusmint.brickball.command.BrickballTestCommand;
import com.github.drfiveminusmint.brickball.listener.PlayerListener;
import com.github.drfiveminusmint.brickball.match.MatchManager;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.brickball.scheduling.BrickballScheduler;
import com.github.drfiveminusmint.brickball.scheduling.CreateMatchTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public final class Brickball extends JavaPlugin {
    private static Brickball instance;
    private static File templatesFolder;
    private TemplateManager templateManager;
    private MatchManager matchManager;
    private World matchWorld;
    private BrickballScheduler scheduler;
    private boolean doBackgroundArenaGeneration = false;

    public static Brickball getInstance() {
        return instance;
    }

    public static File getTemplatesFolder() {
        return templatesFolder;
    }

    public TemplateManager getTemplateManager() { return templateManager; }

    public MatchManager getMatchManager() { return matchManager; }

    public BrickballScheduler getScheduler() { return  scheduler; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Plugin startup logic
        instance = this;
        templatesFolder = new File(this.getDataFolder().getAbsolutePath() + "/templates/");
        if (!templatesFolder.exists()) templatesFolder.mkdirs();
        this.templateManager = new TemplateManager();
        this.matchManager = new MatchManager();
        this.scheduler = new BrickballScheduler();
        MatchSettings.loadDefault(getConfig().getConfigurationSection("defaultSettings"));
        matchWorld = Bukkit.getWorld(getConfig().getString("world", "brickball"));
        if (matchWorld == null)
        {
            getLogger().log(Level.WARNING, "No default world for Brickball found! Define one with /brickball setworld");
        }
        getCommand("brickballtest").setExecutor(new BrickballTestCommand());
        getCommand("brickball").setExecutor(new BrickballCommand());
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // Get the maps
        for (File f : Objects.requireNonNull(templatesFolder.listFiles(pathname -> {
            try {
                if (pathname.getName().contains(".bbmap"))
                    return true;
            } catch (Exception ex) {
                return false;
            }
            return false;
        }))) {
            if (this.templateManager.loadTemplateFromFile(f))
                getLogger().log(Level.INFO, "[Debug] Loaded map " + f.getName());
            else
                getLogger().log(Level.INFO, "[Debug] Couldn't load map " + f.getName());
        }

        doBackgroundArenaGeneration = getConfig().getBoolean("preloadArenas", false);
        if (doBackgroundArenaGeneration) {
            int i = 0;
            for (ArenaTemplate template : templateManager.templates.values()) {
                scheduler.submitTask(new CreateMatchTask(template.getID(), -1));
            }
        }
    }

    public World getMatchWorld() {return matchWorld;}
    public void setMatchWorld(World world) {matchWorld = world;}

    public boolean isBackgroundGenerationEnabled() {
        return doBackgroundArenaGeneration;
    }

    @Override
    public void onDisable() {
        matchManager.stopAllMatches();
    }
}
