package com.ljuangbminecraft.tfcchannelcasting;

import org.slf4j.Logger;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCBlocks;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TFCChannelCasting.MOD_ID)
public class TFCChannelCasting {
    // Directly reference a slf4j logger
    public static final String MOD_ID = "tfcchannelcasting";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TFCChannelCasting() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        TFCCCBlocks.BLOCKS.register(bus);
        TFCCCBlockEntities.BLOCK_ENTITIES.register(bus);
        TFCCCItems.ITEMS.register(bus);
    }
}
