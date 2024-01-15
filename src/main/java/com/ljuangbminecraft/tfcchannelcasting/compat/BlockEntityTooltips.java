package com.ljuangbminecraft.tfcchannelcasting.compat;

import java.util.Optional;

import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.MoldBlockEntity;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.MoldBlock;

import net.dries007.tfc.common.capabilities.MoldLike;
import net.dries007.tfc.compat.jade.common.BlockEntityTooltip;
import net.dries007.tfc.compat.jade.common.RegisterCallback;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.dries007.tfc.config.TFCConfig;

public class BlockEntityTooltips {

    public static void register(RegisterCallback<BlockEntityTooltip, Block> callback)
    {
        callback.register("mold_table", MOLD_TABLE, MoldBlock.class);
    }

    public static final BlockEntityTooltip MOLD_TABLE = (level, state, pos, entity, tooltip) -> {
        if (entity instanceof MoldBlockEntity mold)
        {            
            Optional.ofNullable(mold.getInventory().getStackInSlot(MoldBlockEntity.MOLD_SLOT)).ifPresent(
                drainStack -> Optional.ofNullable(MoldLike.get(drainStack)).ifPresent(          
                    moldItem -> {
                        float temperature = moldItem.getTemperature();
                        final MutableComponent heat = TFCConfig.CLIENT.heatTooltipStyle.get().formatColored(temperature);
                        if (heat != null)
                        {
                            tooltip.accept(heat);
                        }
                    }
                )
            );
        }
    };
}
