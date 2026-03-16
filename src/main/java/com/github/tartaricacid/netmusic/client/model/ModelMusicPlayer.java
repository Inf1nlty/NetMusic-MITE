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

        // Geometry is reduced from resources/models/block/music_player.json for 1.6.4 RenderBlocks rendering.
        add(0.01F, 0.01F, 0.01F, 15.99F, 7.99F, 15.99F);
        add(0.01F, 8.01F, 0.01F, 15.99F, 26.99F, 15.99F);
        add(0.01F, 27.01F, 3.01F, 15.99F, 30.99F, 15.99F);
        add(3.01F, 8.01F, 9.99F, 12.99F, 17.99F, 10.01F);
        add(2.76F, 17.01F, 9.01F, 13.24F, 18.99F, 10.99F);
        add(2.01F, 8.01F, 1.74F, 5.99F, 14.99F, 1.76F);

        // Speaker details on front side.
        add(12.60F, 19.24F, 6.76F, 15.58F, 23.22F, 9.24F);
        add(1.41F, 19.24F, 5.76F, 3.89F, 23.22F, 8.74F);

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