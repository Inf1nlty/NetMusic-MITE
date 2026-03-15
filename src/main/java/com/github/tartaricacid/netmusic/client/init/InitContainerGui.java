package com.github.tartaricacid.netmusic.client.init;

import com.github.tartaricacid.netmusic.client.gui.CDBurnerMenuScreen;
import com.github.tartaricacid.netmusic.client.gui.ComputerMenuScreen;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InitContainerGui {
    private static final Map<Class<?>, Function<Object, Object>> SCREEN_FACTORIES = new HashMap<Class<?>, Function<Object, Object>>();

    public static void init() {
        register(CDBurnerMenu.class, menu -> new CDBurnerMenuScreen((CDBurnerMenu) menu));
        register(ComputerMenu.class, menu -> new ComputerMenuScreen((ComputerMenu) menu));
    }

    public static void register(Class<?> menuClass, Function<Object, Object> factory) {
        if (menuClass != null && factory != null) {
            SCREEN_FACTORIES.put(menuClass, factory);
        }
    }

    public static Object createScreen(Object menu) {
        if (menu == null) {
            return null;
        }
        Function<Object, Object> factory = SCREEN_FACTORIES.get(menu.getClass());
        return factory == null ? null : factory.apply(menu);
    }
}
