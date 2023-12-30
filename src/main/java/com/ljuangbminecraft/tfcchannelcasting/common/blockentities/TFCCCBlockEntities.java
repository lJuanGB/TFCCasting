package com.ljuangbminecraft.tfcchannelcasting.common.blockentities;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;

import java.util.function.Supplier;

import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCBlocks;

import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TFCCCBlockEntities 
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<BlockEntityType<MoldBlockEntity>> MOLD_TABLE = register("mold_table", MoldBlockEntity::new, TFCCCBlocks.MOLD_TABLE);
    public static final RegistryObject<BlockEntityType<ChannelBlockEntity>> CHANNEL = register("channel", ChannelBlockEntity::new, TFCCCBlocks.CHANNEL);

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block)
    {
        return RegistrationHelpers.register(BLOCK_ENTITIES, name, factory, block);
    }
}
