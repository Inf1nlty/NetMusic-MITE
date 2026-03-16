package com.github.tartaricacid.netmusic.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelMusicPlayer {
    public static final String LAYER = "netmusic_music_player";
    private static final List<Cuboid> CUBOIDS = new ArrayList<Cuboid>();

    private ModelMusicPlayer() {
    }

    public static Object createBodyLayer() {
        if (!CUBOIDS.isEmpty()) {
            return CUBOIDS;
        }

        // Base body.
        add(1.5F, 0.0F, 1.5F, 14.5F, 5.5F, 14.5F);
        add(1.0F, 5.5F, 1.0F, 15.0F, 7.0F, 15.0F);

        // Turntable platform and disc.
        add(3.0F, 6.9F, 3.0F, 13.0F, 7.4F, 13.0F);
        add(4.0F, 7.4F, 4.0F, 12.0F, 8.2F, 12.0F);
        add(7.5F, 8.2F, 7.5F, 8.5F, 9.0F, 8.5F);

        // Arm support and arm.
        add(10.8F, 7.0F, 2.2F, 12.4F, 11.8F, 3.8F);
        add(8.4F, 10.8F, 2.4F, 12.2F, 11.6F, 3.6F);
        add(7.8F, 10.6F, 2.8F, 8.6F, 11.4F, 3.4F);

        // Speaker horn cluster.
        add(2.6F, 7.0F, 2.6F, 6.2F, 10.0F, 6.2F);
        add(2.0F, 10.0F, 2.0F, 6.8F, 12.0F, 6.8F);
        add(3.0F, 12.0F, 3.0F, 5.8F, 13.4F, 5.8F);

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

    public static final class Cuboid {
        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;

        private Cuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }
}