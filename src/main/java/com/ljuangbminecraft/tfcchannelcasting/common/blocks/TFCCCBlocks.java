package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.MoldBlockEntity;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TFCCCBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Block> CHANNEL = register(
        "channel",
        () -> new ChannelBlock(
            ExtendedProperties.of(MapColor.METAL).strength(3).sound(SoundType.METAL).blockEntity(TFCCCBlockEntities.CHANNEL)
            .lightLevel(s -> s.getValue(ChannelBlock.WITH_METAL) ? 10 : 0)
        )
    );

    public static final RegistryObject<Block> MOLD_TABLE = register(
        "mold_table",
        () -> new MoldBlock(
            ExtendedProperties.of(MapColor.METAL).strength(3).sound(SoundType.METAL).blockEntity(TFCCCBlockEntities.MOLD_TABLE).serverTicks(MoldBlockEntity::serverTick)
        )
    );

    public static final Map<ExtraFluid, RegistryObject<LiquidBlock>> EXTRA_FLUIDS = Helpers.mapOfKeys(ExtraFluid.class, fluid ->
        registerNoItem("fluid/" + fluid.getSerializedName(), () -> new LiquidBlock(TFCCCFluids.EXTRA_FLUIDS.get(fluid).source(), Properties.copy(Blocks.WATER)))
    );

    private static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> blockSupplier)
    {
        return register(name, blockSupplier, (Function<T, ? extends BlockItem>) null);
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier)
    {
        return register(name, blockSupplier, block -> new BlockItem(block, new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier, @Nullable Function<T, ? extends BlockItem> blockItemFactory)
    {
        return RegistrationHelpers.registerBlock(TFCCCBlocks.BLOCKS, TFCCCItems.ITEMS, name, blockSupplier, blockItemFactory);
    }
}
