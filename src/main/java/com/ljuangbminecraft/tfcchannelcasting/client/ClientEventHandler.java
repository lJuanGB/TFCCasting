package com.ljuangbminecraft.tfcchannelcasting.client;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;
import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientEventHandler
{
    public static void init()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(ClientEventHandler::registerEntityRenderers);
        //bus.addListener(ClientEventHandler::onTextureStitch);
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(TFCCCBlockEntities.CHANNEL.get(), ctx -> new ChannelBlockEntityRenderer());
        event.registerBlockEntityRenderer(TFCCCBlockEntities.MOLD_TABLE.get(), ctx -> new MoldBlockEntityRenderer());
    }
    /*
    public static void onTextureStitch(TextureStitchEvent event)
    {
        final ResourceLocation sheet = event.getAtlas().location();
        if (sheet.equals(RenderHelpers.BLOCKS_ATLAS))
        {
            event.addSprite(new ResourceLocation("tfcchannelcasting", "block/metal/full/dark_chocolate") );
            event.addSprite(new ResourceLocation("tfcchannelcasting", "block/metal/full/milk_chocolate") );
            event.addSprite(new ResourceLocation("tfcchannelcasting", "block/metal/full/white_chocolate") );
        }
    }
    */
}