package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import net.minecraft.resources.ResourceLocation;

import static net.dries007.tfc.common.recipes.outputs.ItemStackModifiers.register;

public class TFCCCItemStackModifiers {
    public static void registerItemStackModifierTypes() {
        register(new ResourceLocation("tfcchannelcasting", "set_food_data"),
                SetFoodDataItemStackModifier.Serializer.INSTANCE);

        register(new ResourceLocation("tfcchannelcasting", "conditional"),
                ConditionalItemStackModifier.Serializer.INSTANCE);
    }
}