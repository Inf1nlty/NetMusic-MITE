package com.github.tartaricacid.netmusic.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelCDBurner {
    public static final String LAYER = "netmusic_cd_burner";
    private static final List<Cuboid> CUBOIDS = new ArrayList<>();

    public ModelCDBurner() {}

    public static Object createBodyLayer() {
        if (!CUBOIDS.isEmpty()) {
            return CUBOIDS;
        }
        // Base body (shorter than music player)
        add(2.0F, 0.0F, 2.0F, 14.0F, 4.0F, 14.0F);
        // Disc slot
        add(5.0F, 4.0F, 5.0F, 11.0F, 5.0F, 11.0F);
        // Button
        add(7.0F, 1.0F, 13.0F, 9.0F, 2.0F, 14.0F);
        return CUBOIDS;
    }

    public static List<Cuboid> getCuboids() {
        if (CUBOIDS.isEmpty()) {
            createBodyLayer();
        }
        return Collections.unmodifiableList(CUBOIDS);
    }

    private static void add(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        CUBOIDS.add(new Cuboid(minX / 16.0F, minY / 16.0F, minZ / 16.0F, maxX / 16.0F, maxY / 16.0F, maxZ / 16.0F));
    }

    public static final class Cuboid implements com.github.tartaricacid.netmusic.client.renderer.CuboidRenderHelper.Cuboid {
        public final float minX, minY, minZ, maxX, maxY, maxZ;
        private Cuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        @Override public float minX() { return minX; }
        @Override public float minY() { return minY; }
        @Override public float minZ() { return minZ; }
        @Override public float maxX() { return maxX; }
        @Override public float maxY() { return maxY; }
        @Override public float maxZ() { return maxZ; }
    }
}

