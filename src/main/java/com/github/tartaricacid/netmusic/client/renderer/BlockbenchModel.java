package com.github.tartaricacid.netmusic.client.renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class BlockbenchModel {
    final ResourceLocation texture;
    final int textureWidth;
    final int textureHeight;
    final List<Element> elements;

    private BlockbenchModel(ResourceLocation texture, int textureWidth, int textureHeight, List<Element> elements) {
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.elements = elements;
    }

    static BlockbenchModel load(String modelPath, ResourceLocation texture, int fallbackTextureWidth, int fallbackTextureHeight) {
        try (InputStream in = BlockbenchModel.class.getClassLoader().getResourceAsStream(modelPath)) {
            if (in == null) {
                return new BlockbenchModel(texture, fallbackTextureWidth, fallbackTextureHeight, new ArrayList<Element>());
            }
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            int texW = fallbackTextureWidth;
            int texH = fallbackTextureHeight;
            JsonArray textureSize = root.getAsJsonArray("texture_size");
            if (textureSize != null && textureSize.size() >= 2) {
                texW = Math.round(textureSize.get(0).getAsFloat());
                texH = Math.round(textureSize.get(1).getAsFloat());
            }

            List<Element> elements = new ArrayList<Element>();
            JsonArray elementArray = root.getAsJsonArray("elements");
            if (elementArray != null) {
                for (JsonElement elementRaw : elementArray) {
                    JsonObject element = elementRaw.getAsJsonObject();
                    float[] from = readVec3(element.getAsJsonArray("from"));
                    float[] to = readVec3(element.getAsJsonArray("to"));
                    Rotation rotation = readRotation(element.getAsJsonObject("rotation"));
                    Face[] faces = readFaces(element.getAsJsonObject("faces"));
                    boolean shade = !element.has("shade") || element.get("shade").getAsBoolean();
                    float minX = Math.min(from[0], to[0]) / 16.0F;
                    float minY = Math.min(from[1], to[1]) / 16.0F;
                    float minZ = Math.min(from[2], to[2]) / 16.0F;
                    float maxX = Math.max(from[0], to[0]) / 16.0F;
                    float maxY = Math.max(from[1], to[1]) / 16.0F;
                    float maxZ = Math.max(from[2], to[2]) / 16.0F;
                    elements.add(new Element(minX, minY, minZ, maxX, maxY, maxZ, rotation, faces, shade));
                }
            }
            return new BlockbenchModel(texture, texW, texH, elements);
        } catch (Exception ignored) {
            return new BlockbenchModel(texture, fallbackTextureWidth, fallbackTextureHeight, new ArrayList<Element>());
        }
    }

    private static float[] readVec3(JsonArray array) {
        if (array == null || array.size() < 3) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static Rotation readRotation(JsonObject rotation) {
        if (rotation == null) {
            return null;
        }
        String axisString = rotation.has("axis") ? rotation.get("axis").getAsString() : "y";
        char axis = StringUtils.isBlank(axisString) ? 'y' : axisString.charAt(0);
        float angle = rotation.has("angle") ? rotation.get("angle").getAsFloat() : 0.0F;
        float[] origin = readVec3(rotation.getAsJsonArray("origin"));
        return new Rotation(axis, angle, origin[0] / 16.0F, origin[1] / 16.0F, origin[2] / 16.0F);
    }

    private static Face[] readFaces(JsonObject facesObject) {
        Face[] faces = new Face[6];
        if (facesObject == null) {
            return faces;
        }
        readFace(facesObject, faces, "down", 0);
        readFace(facesObject, faces, "up", 1);
        readFace(facesObject, faces, "north", 2);
        readFace(facesObject, faces, "south", 3);
        readFace(facesObject, faces, "west", 4);
        readFace(facesObject, faces, "east", 5);
        return faces;
    }

    private static void readFace(JsonObject facesObject, Face[] faces, String key, int index) {
        JsonObject faceObject = facesObject.getAsJsonObject(key);
        if (faceObject == null) {
            return;
        }
        JsonArray uv = faceObject.getAsJsonArray("uv");
        if (uv == null || uv.size() < 4) {
            return;
        }
        int rotation = faceObject.has("rotation") ? faceObject.get("rotation").getAsInt() : 0;
        faces[index] = new Face(
                uv.get(0).getAsFloat(),
                uv.get(1).getAsFloat(),
                uv.get(2).getAsFloat(),
                uv.get(3).getAsFloat(),
                rotation
        );
    }

    static final class Element {
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;
        final Rotation rotation;
        final Face[] faces;
        final boolean shade;

        Element(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                Rotation rotation, Face[] faces, boolean shade) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.rotation = rotation;
            this.faces = faces;
            this.shade = shade;
        }
    }

    static final class Rotation {
        final char axis;
        final float angle;
        final float originX;
        final float originY;
        final float originZ;

        Rotation(char axis, float angle, float originX, float originY, float originZ) {
            this.axis = axis;
            this.angle = angle;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
        }
    }

    static final class Face {
        final float u0;
        final float v0;
        final float u1;
        final float v1;
        final int rotation;

        Face(float u0, float v0, float u1, float v1, int rotation) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.rotation = rotation;
        }
    }
}
