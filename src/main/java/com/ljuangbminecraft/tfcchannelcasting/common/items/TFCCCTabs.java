package com.ljuangbminecraft.tfcchannelcasting.common.items;

import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;

public class TFCCCTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<CreativeModeTab> SSB_TAB = CREATIVE_MODE_TABS.register("casting_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(TFCCCItems.UNFIRED_MOLD_TABLE.get()))
                    .title(Component.translatable("creativetab.casting_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(TFCCCItems.UNFIRED_MOLD_TABLE.get());
                        pOutput.accept(TFCCCItems.UNFIRED_CHANNEL.get());
                        pOutput.accept(TFCCCBlocks.MOLD_TABLE.get());
                        pOutput.accept(TFCCCBlocks.CHANNEL.get());

                    })
                    .build());
    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
