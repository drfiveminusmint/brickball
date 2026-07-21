package com.github.drfiveminusmint.brickball;

import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.command.BrickballCommand;
import com.github.drfiveminusmint.brickball.events.listener.MatchEndListener;
import com.github.drfiveminusmint.brickball.events.listener.PlayerListener;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.lobby.LobbyList;
import com.github.drfiveminusmint.brickball.match.MatchManager;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.brickball.scheduling.BrickballScheduler;
import com.github.drfiveminusmint.brickball.scheduling.CreateMatchTask;
import com.github.drfiveminusmint.brickball.scheduling.LoadStatsTask;
import com.github.drfiveminusmint.brickball.stats.FormatStats;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

public final class Brickball extends JavaPlugin {
    private static Brickball instance;
    private static File templatesFolder, statsFolder;
    private TemplateManager templateManager;
    private MatchManager matchManager;
    private LobbyList lobbyList;
    private ArrayList<BrickballFormat> formats = new ArrayList<>();
    private HashMap<BrickballFormat, FormatStats> perFormatStats = new HashMap();
    private World matchWorld;
    private BrickballScheduler scheduler;
    private boolean doBackgroundArenaGeneration = false;

    public static Brickball getInstance() {
        return instance;
    }

    public static File getTemplatesFolder() {
        return templatesFolder;
    }
    public static File getStatsFolder() {return statsFolder;}

    public TemplateManager getTemplateManager() { return templateManager; }

    public MatchManager getMatchManager() { return matchManager; }
    public LobbyList getLobbyList() { return lobbyList; }

    public BrickballScheduler getScheduler() { return  scheduler; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Plugin startup logic
        instance = this;
        templatesFolder = new File(this.getDataFolder().getAbsolutePath() + "/templates/");
        if (!templatesFolder.exists()) templatesFolder.mkdirs();
        statsFolder = new File(getDataFolder(), "stats");
        if (!statsFolder.exists()) statsFolder.mkdirs();
        // Save default formats if not present
        File formatsFolder = new File(getDataFolder(), "formats");
        if (!formatsFolder.exists()) {
            formatsFolder.mkdirs();
            saveResource("formats/custom.yml", false);
        }
        this.templateManager = new TemplateManager();
        this.matchManager = new MatchManager();
        this.scheduler = new BrickballScheduler();
        this.lobbyList = new LobbyList();
        // Load default match settings
        MatchSettings.loadDefault(getConfig().getConfigurationSection("defaultSettings"));
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
        // Load formats and stats
        for (File file : formatsFolder.listFiles()) {
            YamlConfiguration formatConfig = new YamlConfiguration();
            try {
                formatConfig.load(file);
                BrickballFormat format = new BrickballFormat(formatConfig);
                formats.add(format);
                FormatStats formatStats = new FormatStats(format);
                perFormatStats.put(format, formatStats);
                // load stats from the CSV
                File statsFile = new File(statsFolder, format.getName() + ".csv");
                if (!statsFile.exists()) statsFile.createNewFile();
                scheduler.submitTask(new LoadStatsTask(99, statsFile, formatStats));
                getLogger().log(Level.INFO, "Loaded format " + format.getName());
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, String.format("Error loading format file %s!", file.getName()));
                e.printStackTrace();
            }
        }
        matchWorld = Bukkit.getWorld(getConfig().getString("world", "brickball"));
        if (matchWorld == null)
        {
            getLogger().log(Level.WARNING, "No default world for Brickball found! Define one with /brickball setworld");
        }
        getCommand("brickball").setExecutor(new BrickballCommand());
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new MatchEndListener(), this);

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
    public ArrayList<BrickballFormat> getFormats() { return formats; }
    public FormatStats getFormatStats(BrickballFormat format) {return perFormatStats.get(format);}

    public boolean isBackgroundGenerationEnabled() {
        return doBackgroundArenaGeneration;
    }

    @Override
    public void onDisable() {
        matchManager.stopAllMatches();
        scheduler.shutdown();
    }
}
