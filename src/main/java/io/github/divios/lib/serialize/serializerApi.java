package io.github.divios.lib.serialize;

import com.google.common.base.Preconditions;
import io.github.divios.core_lib.cache.Lazy;
import io.github.divios.core_lib.utils.Log;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.utils.FileUtils;
import io.github.divios.dailyShop.utils.Utils;
import io.github.divios.lib.dLib.dShop;

import java.io.File;
import java.util.Objects;

public class serializerApi {

    private static final DailyShop plugin = DailyShop.getInstance();
    private static final Lazy<File> shopsFolder = Lazy.suppliedBy(() -> new File(plugin.getDataFolder(), "shops"));

    public static void saveShopToFile(dShop shop) {
        try {
            File data = new File(shopsFolder.get(), shop.getName() + ".yml");
            FileUtils.toYaml(dShop.encodeOptions.JSON.toJson(shop), data);
        } catch (Exception e) {
            Log.info("There was a problem saving the shop " + shop.getName());
            e.printStackTrace();
        }
        //Log.info("Converted all items correctly of shop " + shop.getName());
    }

    public static dShop getShopFromFile(File data) {
        Objects.requireNonNull(data, "data cannot be null");
        Preconditions.checkArgument(data.exists(), "The file does not exist");
        return dShop.encodeOptions.JSON.fromJson(Utils.getJsonFromFile(data));
    }

}
