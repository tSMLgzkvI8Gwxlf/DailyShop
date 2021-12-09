package io.github.divios.dailyShop.files;

import com.google.common.collect.Lists;
import io.github.divios.core_lib.utils.Log;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.utils.FileUtils;
import io.github.divios.dailyShop.utils.Timer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.List;

public abstract class resource {

    private static final DailyShop plugin = DailyShop.getInstance();

    private final String name;

    private File file;
    protected YamlConfiguration yaml;
    private Long checkSum;

    protected resource(String name) {
        this.name = name;
        create();
    }

    public void create() {

        file = new File(plugin.getDataFolder(), name);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(name, false);
        }

        Timer timer = Timer.create();
        Log.info(getStartMessage());
        Long checkSumAux;
        if ( (checkSumAux = FileUtils.getFileCheckSum(file)) == checkSum ) { // If same checkSum -> no changes
            Log.info(getCanceledMessage());
            return;
        }
        checkSum = checkSumAux;

        yaml = YamlConfiguration.loadConfiguration(file);
        copyDefaults();

        init();

        timer.stop();
        Log.info(getFinishedMessage(timer.getTime()));
    }

    public void reload() {
        create();
    }

    protected abstract String getStartMessage();

    protected abstract String getCanceledMessage();

    protected abstract String getFinishedMessage(long time);

    protected abstract void init();

    private void copyDefaults() {
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(plugin.getResource(name), "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        YamlConfiguration defConfig = null;
        if (defConfigStream != null) {
            defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            yaml.setDefaults(defConfig);
            yaml.options().copyDefaults(true);
        }

        try { yaml.save(file); }
        catch (IOException e) { e.printStackTrace(); }

        if (defConfig != null) yaml.setDefaults(defConfig);
    }

}