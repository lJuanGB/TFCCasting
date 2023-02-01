package com.ljuangbminecraft.tfcchannelcasting.client;


import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.ChannelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class ChannelBlockEntityRenderer implements BlockEntityRenderer<ChannelBlockEntity>
{
    @Override
    public void render(ChannelBlockEntity channel, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay)
    {
        if (!channel.shouldRender())
        {
            return;
        }
        
        int color = channel.getColor();
        ResourceLocation texture = channel.getTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(texture);

        VertexConsumer builder = buffer.getBuffer(RenderType.cutout());

        FluidRenderHelpers.renderFlowCenter(poseStack, builder, sprite, color, combinedLight, combinedOverlay);
        FluidRenderHelpers.renderFlow(poseStack, builder, sprite, color, combinedLight, combinedOverlay, channel.getFlowSource(), channel.isConnectedToAnotherChannel());
    }
}
