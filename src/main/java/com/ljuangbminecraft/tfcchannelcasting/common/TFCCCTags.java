package com.ljuangbminecraft.tfcchannelcasting.common;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

public class TFCCCTags 
{
    public static class Blocks
    {
    }

    public static class Fluids
    {
        public static final TagKey<Fluid> USABLE_IN_HEART_MOLD  = create("usable_in_heart_mold");

        private static TagKey<Fluid> create(String id)
        {
            return TagKey.create(Registry.FLUID_REGISTRY, new ResourceLocation("tfcchannelcasting", id));
        }
    }

    public static class Items
    {
        public static final TagKey<Item> ACCEPTED_IN_MOLD_TABLES = create("accepted_in_mold_table");

        private static TagKey<Item> create(String id)
        {
            return TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation("tfcchannelcasting", id));
        }
    }
}
