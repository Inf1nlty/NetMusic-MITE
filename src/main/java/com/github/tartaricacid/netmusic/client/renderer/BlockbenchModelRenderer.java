package com.github.tartaricacid.netmusic.client.renderer;

import net.minecraft.Tessellator;
import org.lwjgl.opengl.GL11;

final class BlockbenchModelRenderer {
    private BlockbenchModelRenderer() {
    }

    static void render(BlockbenchModel model, int turnsClockwise) {
        if (model == null || model.elements.isEmpty()) {
            return;
        }
        boolean hasShaded = false;
        boolean hasUnshaded = false;
        for (int i = 0; i < model.elements.size(); i++) {
            BlockbenchModel.Element e = model.elements.get(i);
            if (e.shade) {
                hasShaded = true;
            } else {
                hasUnshaded = true;
            }
            if (hasShaded && hasUnshaded) {
                break;
            }
        }

        if (hasShaded) {
            renderPass(model, turnsClockwise, true);
        }
        if (hasUnshaded) {
            boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
            if (lightingEnabled) {
                GL11.glDisable(GL11.GL_LIGHTING);
            }
            try {
                renderPass(model, turnsClockwise, false);
            } finally {
                if (lightingEnabled) {
                    GL11.glEnable(GL11.GL_LIGHTING);
                }
            }
        }
    }

    private static void renderPass(BlockbenchModel model, int turnsClockwise, boolean shadedElements) {
        Tessellator t = Tessellator.instance;
        boolean started = false;
        for (int i = 0; i < model.elements.size(); i++) {
            BlockbenchModel.Element e = model.elements.get(i);
            if (e.shade != shadedElements) {
                continue;
            }
            if (!started) {
                t.startDrawingQuads();
                started = true;
            }
            renderFace(model, e, e.faces[0], 0, turnsClockwise, t);
            renderFace(model, e, e.faces[1], 1, turnsClockwise, t);
            renderFace(model, e, e.faces[2], 2, turnsClockwise, t);
            renderFace(model, e, e.faces[3], 3, turnsClockwise, t);
            renderFace(model, e, e.faces[4], 4, turnsClockwise, t);
            renderFace(model, e, e.faces[5], 5, turnsClockwise, t);
        }
        if (started) {
            t.draw();
        }
    }

    private static void renderFace(BlockbenchModel model, BlockbenchModel.Element e, BlockbenchModel.Face face,
                                   int side, int turnsClockwise, Tessellator t) {
        if (face == null) {
            return;
        }
        float[][] vertices = buildFaceVertices(e, side);
        for (int i = 0; i < 4; i++) {
            rotateElement(vertices[i], e.rotation);
            rotateBlockY(vertices[i], turnsClockwise);
        }

        float[] normal = computeNormal(vertices);
        t.setNormal(normal[0], normal[1], normal[2]);

        float u0 = face.u0 / model.textureWidth;
        float v0 = face.v0 / model.textureHeight;
        float u1 = face.u1 / model.textureWidth;
        float v1 = face.v1 / model.textureHeight;
        float[] us = new float[]{u0, u1, u1, u0};
        float[] vs = new float[]{v0, v0, v1, v1};
        rotateUv(us, vs, face.rotation);

        t.addVertexWithUV(vertices[0][0], vertices[0][1], vertices[0][2], us[0], vs[0]);
        t.addVertexWithUV(vertices[1][0], vertices[1][1], vertices[1][2], us[1], vs[1]);
        t.addVertexWithUV(vertices[2][0], vertices[2][1], vertices[2][2], us[2], vs[2]);
        t.addVertexWithUV(vertices[3][0], vertices[3][1], vertices[3][2], us[3], vs[3]);
    }

    private static float[][] buildFaceVertices(BlockbenchModel.Element e, int side) {
        float minX = e.minX;
        float minY = e.minY;
        float minZ = e.minZ;
        float maxX = e.maxX;
        float maxY = e.maxY;
        float maxZ = e.maxZ;
        switch (side) {
            case 0: // down
                return new float[][]{
                        {minX, minY, minZ},
                        {maxX, minY, minZ},
                        {maxX, minY, maxZ},
                        {minX, minY, maxZ}
                };
            case 1: // up
                return new float[][]{
                        {minX, maxY, maxZ},
                        {maxX, maxY, maxZ},
                        {maxX, maxY, minZ},
                        {minX, maxY, minZ}
                };
            case 2: // north
                return new float[][]{
                        {minX, maxY, minZ},
                        {maxX, maxY, minZ},
                        {maxX, minY, minZ},
                        {minX, minY, minZ}
                };
            case 3: // south
                return new float[][]{
                        {maxX, maxY, maxZ},
                        {minX, maxY, maxZ},
                        {minX, minY, maxZ},
                        {maxX, minY, maxZ}
                };
            case 4: // west
                return new float[][]{
                        {minX, maxY, maxZ},
                        {minX, maxY, minZ},
                        {minX, minY, minZ},
                        {minX, minY, maxZ}
                };
            case 5: // east
            default:
                return new float[][]{
                        {maxX, maxY, minZ},
                        {maxX, maxY, maxZ},
                        {maxX, minY, maxZ},
                        {maxX, minY, minZ}
                };
        }
    }

    private static void rotateUv(float[] us, float[] vs, int rotation) {
        int turns = ((rotation / 90) % 4 + 4) % 4;
        for (int i = 0; i < turns; i++) {
            float u = us[3];
            float v = vs[3];
            us[3] = us[2];
            vs[3] = vs[2];
            us[2] = us[1];
            vs[2] = vs[1];
            us[1] = us[0];
            vs[1] = vs[0];
            us[0] = u;
            vs[0] = v;
        }
    }

    private static void rotateElement(float[] v, BlockbenchModel.Rotation rotation) {
        if (rotation == null || Math.abs(rotation.angle) < 1.0e-5F) {
            return;
        }
        float angle = (float) Math.toRadians(rotation.angle);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float x = v[0] - rotation.originX;
        float y = v[1] - rotation.originY;
        float z = v[2] - rotation.originZ;
        float nx = x;
        float ny = y;
        float nz = z;
        switch (rotation.axis) {
            case 'x':
            case 'X':
                ny = y * cos - z * sin;
                nz = y * sin + z * cos;
                break;
            case 'z':
            case 'Z':
                nx = x * cos - y * sin;
                ny = x * sin + y * cos;
                break;
            case 'y':
            case 'Y':
            default:
                nx = x * cos + z * sin;
                nz = -x * sin + z * cos;
                break;
        }
        v[0] = nx + rotation.originX;
        v[1] = ny + rotation.originY;
        v[2] = nz + rotation.originZ;
    }

    private static void rotateBlockY(float[] v, int turnsClockwise) {
        int turns = ((turnsClockwise % 4) + 4) % 4;
        if (turns == 0) {
            return;
        }
        float x = v[0] - 0.5F;
        float z = v[2] - 0.5F;
        float nx = x;
        float nz = z;
        if (turns == 1) {
            nx = -z;
            nz = x;
        } else if (turns == 2) {
            nx = -x;
            nz = -z;
        } else if (turns == 3) {
            nx = z;
            nz = -x;
        }
        v[0] = nx + 0.5F;
        v[2] = nz + 0.5F;
    }

    private static float[] computeNormal(float[][] vertices) {
        float ux = vertices[1][0] - vertices[0][0];
        float uy = vertices[1][1] - vertices[0][1];
        float uz = vertices[1][2] - vertices[0][2];
        float vx = vertices[2][0] - vertices[0][0];
        float vy = vertices[2][1] - vertices[0][1];
        float vz = vertices[2][2] - vertices[0][2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-6F) {
            return new float[]{0.0F, 1.0F, 0.0F};
        }
        return new float[]{nx / len, ny / len, nz / len};
    }
}
