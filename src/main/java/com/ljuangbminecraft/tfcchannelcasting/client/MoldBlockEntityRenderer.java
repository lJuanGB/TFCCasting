package com.ljuangbminecraft.tfcchannelcasting.client;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.MoldBlockEntity;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.capabilities.MoldLike;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.util.Metal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class MoldBlockEntityRenderer implements BlockEntityRenderer<MoldBlockEntity>
{
    @Override
    public void render(MoldBlockEntity mold, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay)
    {
        VertexConsumer builder = buffer.getBuffer(RenderType.cutout());

        // Render flow into the mold
        if (mold.hasSource())
        {
            ResourceLocation texture = mold.getSourceFluid().getAttributes().getStillTexture();
            int color = RenderHelpers.getFluidColor(mold.getSourceFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(texture);

            FluidRenderHelpers.renderFlow(poseStack, builder, sprite, color, combinedLight, combinedOverlay, mold.getFlowSource(), true);
            if (mold.getFlowSource().getLeft() == Direction.UP)
            {
                FluidRenderHelpers.renderFlowCenter(poseStack, builder, sprite, color, combinedLight, combinedOverlay);
            }
        }
        
        ItemStack moldStack = mold.getMoldStack();
        MoldLike moldItem = MoldLike.get(moldStack);
        if (moldItem != null)
        {
            FluidStack fluidInTank = moldItem.getFluidInTank(0);

            float fillPercent = 0;
            if (mold.getOutputStack().isEmpty())
            {
                fillPercent = ((float) fluidInTank.getAmount()) / moldItem.getTankCapacity(0);
            }
            else // If there is output, assume fillPercent is 1
            {
                fillPercent = 1;
            }

            if (fillPercent > 0)
            {
                Metal metal;
                if (mold.getOutputStack().isEmpty())
                {
                    metal = Metal.get(fluidInTank.getFluid());
                }
                else
                {
                    HeatingRecipe recipe = HeatingRecipe.getRecipe(mold.getOutputStack());
                    metal = Metal.get(recipe.getDisplayOutputFluid().getFluid());
                }

                // Solid!
                if (moldItem.getTemperature() < metal.getMeltTemperature())
                {
                    TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(metal.getTextureId());

                    RenderHelpers.renderTexturedQuads(
                        poseStack, builder, sprite, combinedLight, combinedOverlay, 
                        RenderHelpers.getYVertices(2f / 16, 1f / 16, 2f / 16, 14f / 16, (1 + fillPercent*0.99f)/16, 14f / 16), 
                        16f * (14f / 16 - 2f / 16), 16f * (14f / 16 - 2f / 16), 0, 0, 1);
                }
                else // fluid
                {
                    RenderHelpers.renderFluidFace(poseStack, fluidInTank, buffer, 2f / 16, 2f / 16, 14f / 16, 14f / 16, (1 + fillPercent*0.99f)/16, combinedOverlay, combinedLight);
                }
            }

            // Render the mold
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            ItemStack moldRenderStack = TFCCCItems.getRenderItem(moldStack.getItem().getRegistryName()).get().getDefaultInstance();
            Minecraft.getInstance().getItemRenderer().renderStatic(moldRenderStack, ItemTransforms.TransformType.FIXED, combinedLight, combinedOverlay, poseStack, buffer, 0);
            poseStack.popPose();
        }        
    }
}