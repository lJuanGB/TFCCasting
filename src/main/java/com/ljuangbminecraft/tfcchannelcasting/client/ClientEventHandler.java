package com.ljuangbminecraft.tfcchannelcasting.client;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;

import java.util.Map;

import com.ljuangbminecraft.tfcchannelcasting.common.TFCCCTags;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;

import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCBlocks;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.dries007.tfc.common.TFCCreativeTabs;

public final class ClientEventHandler
{
    public static void init()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(ClientEventHandler::registerEntityRenderers);
        bus.addListener(ClientEventHandler::addItemsToCreativeTab);
        bus.addListener(ClientEventHandler::modelInit);
        bus.addListener(ClientEventHandler::registerModels);
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(TFCCCBlockEntities.CHANNEL.get(), ctx -> new ChannelBlockEntityRenderer());
        event.registerBlockEntityRenderer(TFCCCBlockEntities.MOLD_TABLE.get(), ctx -> new MoldBlockEntityRenderer());
    }

    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTab() == TFCCreativeTabs.FOOD.tab().get()){
            event.acceptAll(TFCCCItems.CHOCOLATE_SWEET.values().stream().flatMap(map -> map.values().stream().map(reg -> FoodCapability.setStackNonDecaying(reg.get().getDefaultInstance()))).toList());
        }
        if (event.getTab() == TFCCreativeTabs.MISC.tab().get()){
            event.accept(TFCCCItems.UNFIRED_CHANNEL);
            event.accept(TFCCCItems.UNFIRED_MOLD_TABLE);
            event.accept(TFCCCItems.UNFIRED_HEART_MOLD);
            event.accept(TFCCCItems.HEART_MOLD);
            // event.acceptAll(TFCCCItems.EXTRA_FLUID_BUCKETS.values().stream().map(reg -> reg.get().getDefaultInstance()).toList());
            event.accept(TFCCCBlocks.CHANNEL);
            event.accept(TFCCCBlocks.MOLD_TABLE);
        }
    }

    public static void modelInit(ModelEvent.RegisterGeometryLoaders event) {
        MoldsModelLoader.register(event);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> resources = rm.listResources("models/mold", r -> r.getPath().endsWith(".json")); 
		for (ResourceLocation model : resources.keySet()) 
        {
            String path = model.getPath();
            path = path.substring("models/".length(), path.length() - ".json".length());
            event.register(new ResourceLocation(MOD_ID, path));
		}
    }
}