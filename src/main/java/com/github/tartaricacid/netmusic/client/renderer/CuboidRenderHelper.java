package com.github.tartaricacid.netmusic.client.renderer;

import net.minecraft.Icon;
import net.minecraft.RenderBlocks;
import net.minecraft.Tessellator;
import net.minecraft.Block;

import java.util.ArrayList;
import java.util.List;

public final class CuboidRenderHelper {
    // Vanilla side order used by RenderBlocks: 0=down,1=up,2=north(Z-),3=south(Z+),4=west(X-),5=east(X+)
    static final int SIDE_DOWN = 0;
    static final int SIDE_UP = 1;
    static final int SIDE_NORTH = 2;
    static final int SIDE_SOUTH = 3;
    static final int SIDE_WEST = 4;
    static final int SIDE_EAST = 5;

    private static final float EPS = 1.0e-6F;

    private CuboidRenderHelper() {
    }

    public interface Cuboid {
        float minX();
        float minY();
        float minZ();
        float maxX();
        float maxY();
        float maxZ();
    }

    static List<Cuboid> rotateCuboidsY(List<? extends Cuboid> source, int turnsClockwise) {
        int turns = normalizeTurns(turnsClockwise);
        if (turns == 0) {
            return new ArrayList<Cuboid>(source);
        }
        List<Cuboid> result = new ArrayList<Cuboid>(source.size());
        for (Cuboid cuboid : source) {
            result.add(rotateCuboidY(cuboid, turns));
        }
        return result;
    }

    private static Cuboid rotateCuboidY(Cuboid c, int turnsClockwise) {
        switch (turnsClockwise) {
            case 1:
                return of(1.0F - c.maxZ(), c.minY(), c.minX(), 1.0F - c.minZ(), c.maxY(), c.maxX());
            case 2:
                return of(1.0F - c.maxX(), c.minY(), 1.0F - c.maxZ(), 1.0F - c.minX(), c.maxY(), 1.0F - c.minZ());
            case 3:
                return of(c.minZ(), c.minY(), 1.0F - c.maxX(), c.maxZ(), c.maxY(), 1.0F - c.minX());
            default:
                return of(c.minX(), c.minY(), c.minZ(), c.maxX(), c.maxY(), c.maxZ());
        }
    }

    static boolean isFaceOccluded(Cuboid a, int side, List<? extends Cuboid> all) {
        for (int i = 0; i < all.size(); i++) {
            Cuboid b = all.get(i);
            if (b == a) {
                continue;
            }
            switch (side) {
                case SIDE_DOWN:
                    if (nearlyEqual(a.minY(), b.maxY())
                            && covers(b.minX(), b.maxX(), a.minX(), a.maxX())
                            && covers(b.minZ(), b.maxZ(), a.minZ(), a.maxZ())) {
                        return true;
                    }
                    break;
                case SIDE_UP:
                    if (nearlyEqual(a.maxY(), b.minY())
                            && covers(b.minX(), b.maxX(), a.minX(), a.maxX())
                            && covers(b.minZ(), b.maxZ(), a.minZ(), a.maxZ())) {
                        return true;
                    }
                    break;
                case SIDE_NORTH:
                    if (nearlyEqual(a.minZ(), b.maxZ())
                            && covers(b.minX(), b.maxX(), a.minX(), a.maxX())
                            && covers(b.minY(), b.maxY(), a.minY(), a.maxY())) {
                        return true;
                    }
                    break;
                case SIDE_SOUTH:
                    if (nearlyEqual(a.maxZ(), b.minZ())
                            && covers(b.minX(), b.maxX(), a.minX(), a.maxX())
                            && covers(b.minY(), b.maxY(), a.minY(), a.maxY())) {
                        return true;
                    }
                    break;
                case SIDE_WEST:
                    if (nearlyEqual(a.minX(), b.maxX())
                            && covers(b.minZ(), b.maxZ(), a.minZ(), a.maxZ())
                            && covers(b.minY(), b.maxY(), a.minY(), a.maxY())) {
                        return true;
                    }
                    break;
                case SIDE_EAST:
                    if (nearlyEqual(a.maxX(), b.minX())
                            && covers(b.minZ(), b.maxZ(), a.minZ(), a.maxZ())
                            && covers(b.minY(), b.maxY(), a.minY(), a.maxY())) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    static void renderCuboidFacesWorld(RenderBlocks renderer, Block block, int x, int y, int z, int metadata,
                                       Cuboid cuboid, List<? extends Cuboid> all) {
        renderer.setRenderBounds(cuboid.minX(), cuboid.minY(), cuboid.minZ(), cuboid.maxX(), cuboid.maxY(), cuboid.maxZ());

        // Assumes the chunk renderer already started Tessellator and bound the blocks atlas.
        if (!isFaceOccluded(cuboid, SIDE_DOWN, all)) {
            renderer.renderFaceYNeg(block, x, y, z, block.getIcon(SIDE_DOWN, metadata));
        }
        if (!isFaceOccluded(cuboid, SIDE_UP, all)) {
            renderer.renderFaceYPos(block, x, y, z, block.getIcon(SIDE_UP, metadata));
        }
        if (!isFaceOccluded(cuboid, SIDE_NORTH, all)) {
            renderer.renderFaceZNeg(block, x, y, z, block.getIcon(SIDE_NORTH, metadata));
        }
        if (!isFaceOccluded(cuboid, SIDE_SOUTH, all)) {
            renderer.renderFaceZPos(block, x, y, z, block.getIcon(SIDE_SOUTH, metadata));
        }
        if (!isFaceOccluded(cuboid, SIDE_WEST, all)) {
            renderer.renderFaceXNeg(block, x, y, z, block.getIcon(SIDE_WEST, metadata));
        }
        if (!isFaceOccluded(cuboid, SIDE_EAST, all)) {
            renderer.renderFaceXPos(block, x, y, z, block.getIcon(SIDE_EAST, metadata));
        }
    }

    static void renderCuboidFacesImmediate(RenderBlocks renderer, Block block, int metadata,
                                           Cuboid cuboid, List<? extends Cuboid> all) {
        renderer.setRenderBounds(cuboid.minX(), cuboid.minY(), cuboid.minZ(), cuboid.maxX(), cuboid.maxY(), cuboid.maxZ());

        Tessellator tessellator = Tessellator.instance;
        renderFaceImmediate(renderer, block, metadata, cuboid, all, SIDE_DOWN, tessellator);
        renderFaceImmediate(renderer, block, metadata, cuboid, all, SIDE_UP, tessellator);
        renderFaceImmediate(renderer, block, metadata, cuboid, all, SIDE_NORTH, tessellator);
        renderFaceImmediate(renderer, block, metadata, cuboid, all, SIDE_SOUTH, tessellator);
        renderFaceImmediate(renderer, block, metadata, cuboid, all, SIDE_WEST, tessellator);
        renderFaceImmediate(renderer, block, metadata, cuboid, all, SIDE_EAST, tessellator);
    }

    private static void renderFaceImmediate(RenderBlocks renderer, Block block, int metadata,
                                            Cuboid cuboid, List<? extends Cuboid> all, int side, Tessellator tessellator) {
        if (isFaceOccluded(cuboid, side, all)) {
            return;
        }
        Icon icon = block.getIcon(side, metadata);
        tessellator.startDrawingQuads();
        switch (side) {
            case SIDE_DOWN:
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case SIDE_UP:
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case SIDE_NORTH:
                tessellator.setNormal(0.0F, 0.0F, -1.0F);
                renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case SIDE_SOUTH:
                tessellator.setNormal(0.0F, 0.0F, 1.0F);
                renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case SIDE_WEST:
                tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case SIDE_EAST:
                tessellator.setNormal(1.0F, 0.0F, 0.0F);
                renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            default:
                break;
        }
        tessellator.draw();
    }

    private static boolean nearlyEqual(float a, float b) {
        return Math.abs(a - b) <= EPS;
    }

    private static boolean covers(float bMin, float bMax, float aMin, float aMax) {
        return bMin <= aMin + EPS && bMax >= aMax - EPS;
    }

    private static int normalizeTurns(int turnsClockwise) {
        int turns = turnsClockwise % 4;
        if (turns < 0) {
            turns += 4;
        }
        return turns;
    }

    private static Cuboid of(final float minX, final float minY, final float minZ,
                             final float maxX, final float maxY, final float maxZ) {
        return new Cuboid() {
            @Override
            public float minX() {
                return minX;
            }

            @Override
            public float minY() {
                return minY;
            }

            @Override
            public float minZ() {
                return minZ;
            }

            @Override
            public float maxX() {
                return maxX;
            }

            @Override
            public float maxY() {
                return maxY;
            }

            @Override
            public float maxZ() {
                return maxZ;
            }
        };
    }
}
