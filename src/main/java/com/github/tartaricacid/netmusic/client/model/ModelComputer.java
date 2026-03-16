package com.github.tartaricacid.netmusic.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelComputer {
    private static final List<Cuboid> CUBOIDS = new ArrayList<Cuboid>();

    private ModelComputer() {
    }

    public static List<Cuboid> getCuboids() {
        if (CUBOIDS.isEmpty()) {
            createBodyLayer();
        }
        return Collections.unmodifiableList(CUBOIDS);
    }

    private static void createBodyLayer() {
        // Copied from original 1.20 BlockComputer.makeShape().
        add(0.0F, 0.0F, 0.40625F, 1.0F, 0.3125F, 1.0F);
        add(0.1875F, 0.3125F, 0.40625F, 0.8125F, 0.375F, 0.875F);
        add(0.125F, 0.375F, 0.53125F, 0.875F, 0.84375F, 0.9375F);
        add(0.1250625F, 0.5608175F, 0.47502125F, 0.8749375F, 0.9356925F, 0.88114625F);
        add(0.1875F, 0.4375F, 0.40625F, 0.8125F, 0.9375F, 0.59375F);
        add(0.0625F, 0.3125F, 0.34375F, 0.9375F, 0.4375F, 0.59375F);
        add(0.0625F, 0.9375F, 0.34375F, 0.9375F, 1.0625F, 0.59375F);
        add(0.8125F, 0.4375F, 0.34375F, 0.9375F, 0.9375F, 0.59375F);
        add(0.0625F, 0.4375F, 0.34375F, 0.1875F, 0.9375F, 0.59375F);
        add(0.0F, 0.0625F, 0.03125F, 1.0F, 0.1875F, 0.375F);
    }

    private static void add(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        CUBOIDS.add(new Cuboid(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public static final class Cuboid implements com.github.tartaricacid.netmusic.client.renderer.CuboidRenderHelper.Cuboid {
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

        @Override public float minX() { return minX; }
        @Override public float minY() { return minY; }
        @Override public float minZ() { return minZ; }
        @Override public float maxX() { return maxX; }
        @Override public float maxY() { return maxY; }
        @Override public float maxZ() { return maxZ; }
    }
}

