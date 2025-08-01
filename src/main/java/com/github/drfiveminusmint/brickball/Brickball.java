package com.github.drfiveminusmint.brickball;

import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.command.BrickballCommand;
import com.github.drfiveminusmint.brickball.command.BrickballTestCommand;
import com.github.drfiveminusmint.brickball.listener.PlayerListener;
import com.github.drfiveminusmint.brickball.match.MatchManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class Brickball extends JavaPlugin {
    private static Brickball instance;
    private static File templatesFolder;
    private TemplateManager templateManager;
    private MatchManager matchManager;

    public static Brickball getInstance() {
        return instance;
    }

    public static File getTemplatesFolder() {
        return templatesFolder;
    }

    public TemplateManager getTemplateManager() {return templateManager;}

    public MatchManager getMatchManager() {return matchManager;}

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        templatesFolder = new File(this.getDataFolder().getAbsolutePath() + "/templates/");
        if (!templatesFolder.exists()) templatesFolder.mkdirs();
        this.templateManager = new TemplateManager();
        this.matchManager = new MatchManager();
        getCommand("brickballtest").setExecutor(new BrickballTestCommand());
        getCommand("brickball").setExecutor(new BrickballCommand());
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        for (File f : Objects.requireNonNull(templatesFolder.listFiles(pathname -> {
            getLogger().log(Level.INFO, "[debug] " + pathname.getName());
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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
