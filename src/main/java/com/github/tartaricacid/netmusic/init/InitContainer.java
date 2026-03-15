package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;

import java.util.HashMap;
import java.util.Map;

public class InitContainer {
    private static final Map<String, Class<?>> CONTAINER_TYPES = new HashMap<String, Class<?>>();

    public static void init() {
        register("cd_burner", CDBurnerMenu.class);
        register("computer", ComputerMenu.class);
    }

    public static void register(String id, Class<?> type) {
        if (id != null && type != null) {
            CONTAINER_TYPES.put(id, type);
        }
    }

    public static Class<?> getType(String id) {
        return CONTAINER_TYPES.get(id);
    }

    public static boolean contains(String id) {
        return CONTAINER_TYPES.containsKey(id);
    }
}
