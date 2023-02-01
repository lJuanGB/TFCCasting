package com.ljuangbminecraft.tfcchannelcasting.common;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TFCCCTags 
{
    public static class Blocks
    {
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
