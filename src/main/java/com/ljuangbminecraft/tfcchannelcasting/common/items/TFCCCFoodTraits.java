package com.ljuangbminecraft.tfcchannelcasting.common.items;

import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.minecraft.resources.ResourceLocation;

public class TFCCCFoodTraits {
    public static void init() {
    }

    public static final FoodTrait FESTIVE = register("festive", 1f);
    public static final FoodTrait SCARY = register("scary", 1f);
    public static final FoodTrait ROMANTIC = register("romantic", 1f);
    public static final FoodTrait FILLED_WITH_JAM = register("filled_with_jam", 1f);
    public static final FoodTrait FILLED_WITH_SWEET_LIQUOR = register("filled_with_sweet_liquor", 1f);
    public static final FoodTrait FILLED_WITH_STRONG_LIQUOR = register("filled_with_strong_liquor", 1f);
    public static final FoodTrait FILLED_WITH_WHISKEY = register("filled_with_whiskey", 1f);

    private static FoodTrait register(String name, float mod) {
        return FoodTrait.register(new ResourceLocation("tfcchannelcasting", name),
                new FoodTrait(mod, "tfcchannelcasting.tooltip.food_trait." + name));
    }
}
