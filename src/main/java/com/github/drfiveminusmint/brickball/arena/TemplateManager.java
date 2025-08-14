package com.github.drfiveminusmint.brickball.arena;

import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TemplateManager {
    public ConcurrentHashMap<String, ArenaTemplate> templates = new ConcurrentHashMap<>();
    public boolean registerTemplate(ArenaTemplate template) {
        if (templates.contains(template)) return false;
        templates.put(template.getID(), template);
        saveTemplateToFile(template);
        return true;
    }
    public boolean deleteTemplate(String id) {
        return templates.remove(id) == null;
    }
    @Nullable
    public ArenaTemplate findTemplate (String id) {
        return templates.get(id);
    }

    public List<String> listTemplateIDs() {
        // I cannot fucking believe there is not a better way to do this
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, ArenaTemplate> entry : templates.entrySet())
            result.add(entry.getKey());
        return result;
    }

    public boolean loadTemplateFromFile (File target) {
        if (!target.exists()) return false;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(target));
            String worldName = reader.readLine();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Brickball.getInstance().getLogger().log(Level.SEVERE,
                        String.format("Error loading Brickball map %s: Cannot find world \"%s\"",target.getName(), worldName));
                return false;
            }
            String[] locationRaw = reader.readLine().split(",");
            if (locationRaw.length != 3) {
                Brickball.getInstance().getLogger().log(Level.SEVERE,
                        String.format("Error loading Brickball map %s: Invalid file format.",target.getName()));
                return false;
            }
            Location brickSpawn = new Location(world,
                    Double.parseDouble(locationRaw[0]),
                    Double.parseDouble(locationRaw[1]),
                    Double.parseDouble(locationRaw[2]));
            return registerTemplate(ArenaTemplate.createByID(target.getName().replaceAll(".bbmap", ""),brickSpawn, new BukkitWorld(world)));
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean saveTemplateToFile(ArenaTemplate template) {
        File target = new File(Brickball.getTemplatesFolder(), template.getID() + ".bbmap");
        if (target.exists()) target.delete();
        BufferedWriter writer;
        try {
            target.createNewFile();
            writer = new BufferedWriter(new FileWriter(target));
            writer.write(template.getBrickSpawn().getWorld().getName());
            writer.newLine();
            writer.write(String.format("%.2f,%.2f,%.2f,", template.getBrickSpawn().x(),
                    template.getBrickSpawn().y(), template.getBrickSpawn().z()));
            writer.close();
        } catch (IOException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE,"Error saving template " + template.getID());
            return false;
        }
        return true;
    }
}
