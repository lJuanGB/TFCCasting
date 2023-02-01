package com.ljuangbminecraft.tfcchannelcasting.client;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientEventHandler
{
    public static void init()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(ClientEventHandler::registerEntityRenderers);
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(TFCCCBlockEntities.CHANNEL.get(), ctx -> new ChannelBlockEntityRenderer());
        event.registerBlockEntityRenderer(TFCCCBlockEntities.MOLD_TABLE.get(), ctx -> new MoldBlockEntityRenderer());
    }
}