package com.ljuangbminecraft.tfcchannelcasting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/*
 * This class re-implements some methods from tfc's RenderHelpers so that a small change
 * on renderTexturedVertex is possible.
 * 
 * Moreover, it handles rendering of the flow in channels and mold tables.
 */
public class FluidRenderHelpers 
{
    public static void renderTexturedCuboid(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int packedLight, int packedOverlay, AABB bounds)
    {
        renderTexturedCuboid(poseStack, buffer, sprite, color, packedLight, packedOverlay, (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ);
    }

    public static void renderTexturedCuboid(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int packedLight, int packedOverlay, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        renderTexturedCuboid(poseStack, buffer, sprite, color, packedLight, packedOverlay, minX, minY, minZ, maxX, maxY, maxZ, 16f * (maxX - minX), 16f * (maxY - minY), 16f * (maxZ - minZ));
    }

    public static void renderTexturedCuboid(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int packedLight, int packedOverlay, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float xPixels, float yPixels, float zPixels)
    {
        renderTexturedQuads(poseStack, buffer, sprite, color, packedLight, packedOverlay, RenderHelpers.getXVertices(minX, minY, minZ, maxX, maxY, maxZ), zPixels, yPixels, 1, 0, 0);
        renderTexturedQuads(poseStack, buffer, sprite, color, packedLight, packedOverlay, RenderHelpers.getYVertices(minX, minY, minZ, maxX, maxY, maxZ), zPixels, xPixels, 0, 1, 0);
        renderTexturedQuads(poseStack, buffer, sprite, color, packedLight, packedOverlay, RenderHelpers.getZVertices(minX, minY, minZ, maxX, maxY, maxZ), xPixels, yPixels, 0, 0, 1);
    }

    public static void renderTexturedQuads(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int packedLight, int packedOverlay, float[][] vertices, float uSize, float vSize, float normalX, float normalY, float normalZ)
    {
        for (float[] v : vertices)
        {
            renderTexturedVertex(poseStack, buffer, color, packedLight, packedOverlay, v[0], v[1], v[2], sprite.getU(v[3] * uSize), sprite.getV(v[4] * vSize), v[5] * normalX, v[5] * normalY, v[5] * normalZ);
        }
    }

    public static void renderTexturedVertex(PoseStack poseStack, VertexConsumer buffer, int color, int packedLight, int packedOverlay, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ)
    {
        buffer.vertex(poseStack.last().pose(), x, y, z)
            .color(color)
            .uv(u, v)
            .uv2(packedLight)
            .overlayCoords(packedOverlay)
            .normal(poseStack.last().normal(), normalX, normalY, normalZ)
            .endVertex();
    }

    public static void renderFlow(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int packedLight, int packedOverlay, Direction dir, boolean renderFlowSource)
    {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5); // Center so that rotation gets applied correctly

        VoxelShape renderBox;
        switch (dir)
        {
            case UP:  
                renderBox = Block.box(6.0f,  4.0f, 6.0f,  10.0f, 16.0f, 10.0f);
                break;
            case SOUTH: poseStack.mulPose(Vector3f.YP.rotationDegrees(90f)); // Combined rotation: 270
            case WEST:  poseStack.mulPose(Vector3f.YP.rotationDegrees(90f)); // Combined rotation: 180
            case NORTH: poseStack.mulPose(Vector3f.YP.rotationDegrees(90f)); // Combined rotation: 90
            case EAST:  
                renderBox = Block.box(
                    10.0f, 1.0f, 6.0f, 
                    renderFlowSource ? 22.0f : 17.0f, 4.0f, 10.0f); // Render a longer box that expands to the source channel if renderFlowSource
                break;
            default:
                throw new IllegalArgumentException("Cannot render source from direction DOWN");
        }
        poseStack.translate(-0.5, 0, -0.5); // Undo translation

        FluidRenderHelpers.renderTexturedCuboid(poseStack, buffer, sprite, color, packedLight, packedOverlay, renderBox.bounds());
        poseStack.popPose();
    }

    public static void renderFlowCenter(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int packedLight, int packedOverlay)
    {
        VoxelShape renderBox = Block.box(6.0f,  1.0f, 6.0f,  10.0f, 4.0f, 10.0f);
        FluidRenderHelpers.renderTexturedCuboid(poseStack, buffer, sprite, color, packedLight, packedOverlay, renderBox.bounds());
    }

}
