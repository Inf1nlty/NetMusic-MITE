package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.EntityPlayer;

public final class MenuSongWriter {
    private MenuSongWriter() {
    }

    public static WriteResult tryWriteToSourceMenu(EntityPlayer player, PendingSongTracker.Source source, ItemMusicCD.SongInfo songInfo) {
        if (player == null || source == null) {
            return WriteResult.noMenu();
        }

        ItemMusicCD.SongInfo sanitized = SongInfoHelper.sanitize(songInfo);
        if (sanitized == null) {
            return WriteResult.fail("gui.netmusic.computer.url.error");
        }

        if (source == PendingSongTracker.Source.CD_BURNER) {
            if (player.openContainer instanceof CDBurnerMenu menu) {
                return fromFailure(menu.tryWriteSong(sanitized));
            }
            return WriteResult.noMenu();
        }

        if (source == PendingSongTracker.Source.COMPUTER) {
            if (player.openContainer instanceof ComputerMenu menu) {
                return fromFailure(menu.tryWriteSong(sanitized));
            }
            return WriteResult.noMenu();
        }
        return WriteResult.noMenu();
    }

    public static WriteResult tryWriteToAnyOpenMenu(EntityPlayer player, ItemMusicCD.SongInfo songInfo) {
        if (player == null) {
            return WriteResult.noMenu();
        }

        ItemMusicCD.SongInfo sanitized = SongInfoHelper.sanitize(songInfo);
        if (sanitized == null) {
            return WriteResult.fail("gui.netmusic.computer.url.error");
        }

        if (player.openContainer instanceof CDBurnerMenu menu) {
            return fromFailure(menu.tryWriteSong(sanitized));
        }
        if (player.openContainer instanceof ComputerMenu menu) {
            return fromFailure(menu.tryWriteSong(sanitized));
        }
        return WriteResult.noMenu();
    }

    private static WriteResult fromFailure(String failure) {
        if (failure == null) {
            return WriteResult.success();
        }
        return WriteResult.fail(failure);
    }

    public static final class WriteResult {
        public enum State {
            NO_MENU,
            SUCCESS,
            FAILURE
        }

        public final State state;
        public final String failureKey;

        private WriteResult(State state, String failureKey) {
            this.state = state;
            this.failureKey = failureKey;
        }

        public static WriteResult noMenu() {
            return new WriteResult(State.NO_MENU, null);
        }

        public static WriteResult success() {
            return new WriteResult(State.SUCCESS, null);
        }

        public static WriteResult fail(String failureKey) {
            return new WriteResult(State.FAILURE, failureKey == null ? "gui.netmusic.computer.url.error" : failureKey);
        }

        public boolean isSuccess() {
            return this.state == State.SUCCESS;
        }

        public boolean isFailure() {
            return this.state == State.FAILURE;
        }

        public boolean isNoMenu() {
            return this.state == State.NO_MENU;
        }
    }
}
