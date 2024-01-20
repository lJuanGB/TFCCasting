package com.ljuangbminecraft.tfcchannelcasting.common.items;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.LOGGER;
import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.function.Supplier;
import com.ljuangbminecraft.tfcchannelcasting.common.TFCCCTags;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.ExtraFluid;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCFluids;
import com.ljuangbminecraft.tfcchannelcasting.config.TFCCCConfig;
import net.dries007.tfc.common.items.MoldItem;
import net.dries007.tfc.util.Helpers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TFCCCItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> UNFIRED_CHANNEL = register("unfired_channel");
    public static final RegistryObject<Item> UNFIRED_MOLD_TABLE = register("unfired_mold_table");

    public static final RegistryObject<Item> UNFIRED_HEART_MOLD = register("unfired_heart_mold");
    public static final RegistryObject<Item> HEART_MOLD = register("heart_mold",
            () -> new MoldItem(
                TFCCCConfig.SERVER.moldHeartCapacity, 
                TFCCCTags.Fluids.USABLE_IN_HEART_MOLD, 
                new Item.Properties())
            );

    public static final Map<ExtraFluid, RegistryObject<BucketItem>> EXTRA_FLUID_BUCKETS = Helpers.mapOfKeys(
            ExtraFluid.class,
            fluid -> register("bucket/" + fluid.getSerializedName(),
                    () -> new BucketItem(TFCCCFluids.EXTRA_FLUIDS.get(fluid).source(),
                            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))));

    public static final Map<ChocolateType, Map<ChocolateSweetType, RegistryObject<ChocolateSweetItem>>> CHOCOLATE_SWEET = Helpers
            .mapOfKeys(
                    ChocolateType.class,
                    chocolate_type -> Helpers.mapOfKeys(
                            ChocolateSweetType.class,
                            sweet_type -> register(
                                    "food/" + chocolate_type.id + "_chocolate_" + sweet_type.id,
                                    () -> new ChocolateSweetItem(new Item.Properties()
                                            .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3f).fast()
                                                    .build())))));

    private static RegistryObject<Item> register(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> item) {
        return ITEMS.register(name.toLowerCase(Locale.ROOT), item);
    }
}
