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
import com.github.drfiveminusmint.brickball.scheduling.matchmaking.Matchmaker;
import com.github.drfiveminusmint.brickball.stats.FormatStats;
import com.github.drfiveminusmint.brickball.stats.Leaderboard;
import com.github.drfiveminusmint.brickball.ui.LobbyCreationMenu;
import com.github.drfiveminusmint.fiveUI.FiveUI;
import com.github.drfiveminusmint.fiveUI.container.Page;
import com.github.drfiveminusmint.fiveUI.element.LinkButton;
import com.github.drfiveminusmint.fiveUI.element.StaticButton;
import com.github.drfiveminusmint.fiveUI.util.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
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
    private final ArrayList<BrickballFormat> formats = new ArrayList<>();
    private final ArrayList<ArenaTemplate> backroundGenerateMaps = new ArrayList<>();
    private final HashMap<BrickballFormat, FormatStats> perFormatStats = new HashMap<>();
    private final HashMap<BrickballFormat, Matchmaker> matchmakers = new HashMap<>();
    private final HashMap<BrickballFormat, Leaderboard> leaderboards = new HashMap<>();
    private World matchWorld, lobbyWorld;
    private BrickballScheduler scheduler;
    private Page mainUI, matchmakingUI, leaderboardUI;
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
        loadTemplates();

        // Load formats and stats
        loadFormats();

        // Setup match and lobby worlds
        matchWorld = Bukkit.getWorld(getConfig().getString("world", "brickball"));
        if (matchWorld == null)
        {
            getLogger().log(Level.WARNING, "No default world for Brickball found! Define one with /brickball setworld");
        }
        lobbyWorld = Bukkit.getWorld(getConfig().getString("lobbyWorld", "world"));

        // Register commands and events
        getCommand("brickball").setExecutor(new BrickballCommand());
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new MatchEndListener(), this);

        // Setup background generation
        for (Object o : getConfig().getList("backgroundGenerateMaps", new ArrayList<>())) {
            if (o instanceof String s && templateManager.findTemplate(s) != null)
                backroundGenerateMaps.add(templateManager.findTemplate(s));
        }
        doBackgroundArenaGeneration = !backroundGenerateMaps.isEmpty();
        if (doBackgroundArenaGeneration) {
            for (ArenaTemplate template : backroundGenerateMaps) {
                scheduler.submitTask(new CreateMatchTask(template.getID(), 0));
            }
        }

        // Create UI Pages
        mainUI = new Page(Component.text("Brickball", NamedTextColor.DARK_RED, TextDecoration.BOLD), InventoryType.CHEST);
        matchmakingUI = new Page(Component.text("Find Match", NamedTextColor.DARK_RED, TextDecoration.BOLD), InventoryType.CHEST);
        leaderboardUI = new Page(Component.text("Leaderboards", NamedTextColor.GOLD, TextDecoration.BOLD), InventoryType.CHEST);

        // Build main UI page
        mainUI.setElement(10, new LinkButton(
                new ItemStackBuilder(Material.BRICK, 1)
                .name(Component.text("Find Match", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .itemStack(),
                matchmakingUI));
        mainUI.setElement(13, new LinkButton(
                new ItemStackBuilder(Material.NETHER_STAR, 1)
                        .name(Component.text("Leaderboards", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                        .itemStack(),
                leaderboardUI));
        StaticButton createCustomButton = new StaticButton(new ItemStackBuilder(Material.ANVIL, 1)
                .name(Component.text("Create Custom Match", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .itemStack());
        createCustomButton.setOnClick(((player, clickableElement, clickType) -> new LobbyCreationMenu(player)));
        mainUI.setElement(16, createCustomButton);

        // Build matchmaking UI page
        createMatchmakingUI();
        matchmakingUI.setOnClose(((player, container, reason) ->  {
            if (reason != InventoryCloseEvent.Reason.PLUGIN)
                Bukkit.getScheduler().runTaskLater(this, () -> mainUI.display(player), 1);
        }));

        // Build leaderboards page
        createLeaderboardsUI();
        leaderboardUI.setOnClose(((player, container, reason) ->  {
            if (reason != InventoryCloseEvent.Reason.PLUGIN)
                Bukkit.getScheduler().runTaskLater(this, () -> mainUI.display(player), 1);
        }));

    }

    public World getMatchWorld() {return matchWorld;}

    public World getLobbyWorld() {return lobbyWorld;}

    public void setMatchWorld(World world) {matchWorld = world;}
    public ArrayList<BrickballFormat> getFormats() { return formats; }
    public FormatStats getFormatStats(BrickballFormat format) {return perFormatStats.get(format);}

    public boolean startPlayerQueue(Player player, BrickballFormat format) {
        // check to see if the player can join the queue
        for (Matchmaker matchmaker : matchmakers.values())
            if (matchmaker.hasPlayer(player))
                return false;
        if (lobbyList.getLobbyByPlayer(player) != null)
            return false;
        // add the player to the queue
        matchmakers.get(format).addPlayer(player);
        player.sendMessage(Component.text("Joined the queue for ", NamedTextColor.GOLD)
                .append(Component.text(format.getName(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("To leave the queue use ", NamedTextColor.GOLD)
                .append( Component.text("/brickball leave", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/bb leave"))));
        return true;
    }

    public boolean endPlayerQueue(Player player) {
        boolean result = false;
        for (Matchmaker matchmaker : matchmakers.values())
            result |= matchmaker.removePlayer(player);
        return result;
    }

    public Leaderboard getLeaderboard(BrickballFormat format) { return leaderboards.get(format); }

    public boolean isBackgroundGenerationEnabled() {
        return doBackgroundArenaGeneration;
    }

    public ArrayList<ArenaTemplate> getBackroundGenerateMaps() {
        return backroundGenerateMaps;
    }

    public Page getMainUI() { return mainUI; }

    @Override
    public void onDisable() {
        FiveUI.getInstance().getUIManager().unregisterInterface(mainUI);
        matchManager.stopAllMatches();
        scheduler.shutdown();
        for (Matchmaker matchmaker : matchmakers.values())
            matchmaker.cancel();
    }

    public void reloadTemplates() {
        templateManager.clearTemplates();
        loadTemplates();
    }

    public void reloadFormats() {
        // shutdown everything
        matchManager.stopAllMatches();
        lobbyList.shutdownAll();
        for (Matchmaker matchmaker : matchmakers.values())
            matchmaker.cancel();
        for (Leaderboard leaderboard : leaderboards.values())
            FiveUI.getInstance().getUIManager().unregisterInterface(leaderboard.getDisplayPage());
        leaderboards.clear();
        matchmakers.clear();
        formats.clear();

        // reload
        loadFormats();
        createLeaderboardsUI();
        createMatchmakingUI();
    }

    private void loadTemplates() {
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
    }

    private void loadFormats() {
        File formatsFolder = new File(getDataFolder(), "formats");
        int numMatchmakers = 0;
        for (File file : formatsFolder.listFiles()) {
            if (file.isDirectory()) continue;
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
                scheduler.submitTask(new LoadStatsTask(99, statsFile, formatStats, format));
                // create matchmakers if necessary
                if (format.getDoMatchmaking()) {
                    Matchmaker matchmaker = new Matchmaker(format);
                    // stagger our matchmakers running
                    matchmaker.runTaskTimerAsynchronously(this, 20 + numMatchmakers++, 20);
                    matchmakers.put(format, matchmaker);
                }
                // Create leaderboards for rated modes
                if (format.getIsRated()) {
                    Leaderboard board = new Leaderboard(format);
                    leaderboards.put(format, board);
                }
                getLogger().log(Level.INFO, "Loaded format " + format.getName());
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, String.format("Error loading format file %s!", file.getName()));
                e.printStackTrace();
            }
        }
    }

    private void createMatchmakingUI() {
        // clear any pre-existing elements
        matchmakingUI.fillElement(null);
        // create a button for each matchmade format
        int i = 0;
        for (BrickballFormat format : matchmakers.keySet()) {
            StaticButton button = new StaticButton(format.getUnselectedDisplayItem());
            button.setOnClick(((player, clickableElement, clickType) -> {
                startPlayerQueue(player, format);
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            }));
            matchmakingUI.setElement(i++, button);
            if (i >= matchmakingUI.getInventory().getSize())
                break;
        }
    }

    private void createLeaderboardsUI() {
        // clear any pre-existing elements
        leaderboardUI.fillElement(null);
        int i = 0;
        for (BrickballFormat format : leaderboards.keySet()) {
            leaderboardUI.setElement(i++, new LinkButton(format.getUnselectedDisplayItem(), leaderboards.get(format).getDisplayPage()));
            // Link the leaderboard page back to the main leaderboards UI page
            leaderboards.get(format).getDisplayPage().setOnClose(((player, container, reason) -> {
                if (reason != InventoryCloseEvent.Reason.PLUGIN)
                    Bukkit.getScheduler().runTaskLater(this, () -> leaderboardUI.display(player), 1);
            }));
            if (i >= leaderboardUI.getInventory().getSize())
                break;
        }
    }
}
