package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;
import static net.dries007.tfc.common.TFCItemGroup.DECORATIONS;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.MoldBlockEntity;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TFCCCBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Block> CHANNEL = register(
        "channel",
        () -> new ChannelBlock(
            ExtendedProperties.of(Material.METAL).strength(3).sound(SoundType.METAL)
        ),
        DECORATIONS
        );

    public static final RegistryObject<Block> MOLD_TABLE = register(
        "mold_table",
        () -> new MoldBlock(
            ExtendedProperties.of(Material.METAL).strength(3).sound(SoundType.METAL).blockEntity(TFCCCBlockEntities.MOLD_TABLE).serverTicks(MoldBlockEntity::serverTick)
        ),
        DECORATIONS
        ); 

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier, CreativeModeTab group)
    {
        return register(name, blockSupplier, block -> new BlockItem(block, new Item.Properties().tab(group)));
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier, @Nullable Function<T, ? extends BlockItem> blockItemFactory)
    {
        return RegistrationHelpers.registerBlock(TFCCCBlocks.BLOCKS, TFCCCItems.ITEMS, name, blockSupplier, blockItemFactory);
    }
}
