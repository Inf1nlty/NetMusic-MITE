package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.inventory.ComputerMenu;

public class ComputerMenuScreen {
    private final ComputerMenu menu;

    public ComputerMenuScreen(ComputerMenu menu) {
        this.menu = menu;
    }

    public ComputerMenu getMenu() {
        return this.menu;
    }
}
