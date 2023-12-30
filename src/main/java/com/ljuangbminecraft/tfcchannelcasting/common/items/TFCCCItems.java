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
            () -> new MoldItem(() -> 100, TFCCCTags.Fluids.USABLE_IN_HEART_MOLD, new Item.Properties()));

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

    private static final HashMap<ResourceLocation, RegistryObject<Item>> moldStackToRenderItem = new HashMap<>();

    static {
        registerRenderItem("tfc:ceramic/ingot_mold", register("mold/ingot"));
        registerRenderItem("tfc:ceramic/pickaxe_head_mold", register("mold/pickaxe_head"));
        registerRenderItem("tfc:ceramic/propick_head_mold", register("mold/propick_head"));
        registerRenderItem("tfc:ceramic/axe_head_mold", register("mold/axe_head"));
        registerRenderItem("tfc:ceramic/shovel_head_mold", register("mold/shovel_head"));
        registerRenderItem("tfc:ceramic/hoe_head_mold", register("mold/hoe_head"));
        registerRenderItem("tfc:ceramic/chisel_head_mold", register("mold/chisel_head"));
        registerRenderItem("tfc:ceramic/hammer_head_mold", register("mold/hammer_head"));
        registerRenderItem("tfc:ceramic/saw_blade_mold", register("mold/saw_blade"));
        registerRenderItem("tfc:ceramic/javelin_head_mold", register("mold/javelin_head"));
        registerRenderItem("tfc:ceramic/sword_blade_mold", register("mold/sword_blade"));
        registerRenderItem("tfc:ceramic/mace_head_mold", register("mold/mace_head"));
        registerRenderItem("tfc:ceramic/knife_blade_mold", register("mold/knife_blade"));
        registerRenderItem("tfc:ceramic/scythe_blade_mold", register("mold/scythe_blade"));
        registerRenderItem("tfc:ceramic/fire_ingot_mold", register("mold/fire_ingot"));
        registerRenderItem("tfc:ceramic/bell_mold", register("mold/bell"));
        registerRenderItem("tfcchannelcasting:heart_mold", register("mold/heart"));
    }

    /***
     * Register the item that should be used to render a ceramic mold
     * in the mold table.
     * 
     * @param loc  the registry name of the item for which a render
     *             is being registered (for example, "tfc:ceramic/ingot_mold")
     * @param item the item that should be used to render the mold
     */
    public static void registerRenderItem(ResourceLocation loc, RegistryObject<Item> item) {
        moldStackToRenderItem.put(loc, item);
    }

    private static void registerRenderItem(String loc, RegistryObject<Item> item) {
        registerRenderItem(new ResourceLocation(loc), item);
    }

    public static Optional<RegistryObject<Item>> getRenderItem(ResourceLocation loc) {
        if (!moldStackToRenderItem.containsKey(loc)) {
            LOGGER.warn("Cannot render %s because no render item is registered!".formatted(loc.toString()));
            return Optional.empty();
        }
        return Optional.of(moldStackToRenderItem.get(loc));
    }

    private static RegistryObject<Item> register(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> item) {
        return ITEMS.register(name.toLowerCase(Locale.ROOT), item);
    }
}
