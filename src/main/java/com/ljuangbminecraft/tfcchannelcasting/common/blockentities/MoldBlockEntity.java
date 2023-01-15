package com.ljuangbminecraft.tfcchannelcasting.common.blockentities;

import java.util.Optional;

import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.MoldLike;
import net.dries007.tfc.common.capabilities.PartialItemHandler;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class MoldBlockEntity extends TickableInventoryBlockEntity<ItemStackHandler>
{

    public static void serverTick(Level level, BlockPos pos, BlockState state, MoldBlockEntity mold)
    {
        if (!mold.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty())
        {
            return;
        }

        final ItemStack drainStack = mold.inventory.getStackInSlot(MOLD_SLOT);
        MoldLike moldItem = MoldLike.get(drainStack);
        if (moldItem != null && !moldItem.isMolten())
        {
            final CastingRecipe recipe = CastingRecipe.get(moldItem);
            if (recipe != null)
            {
                Optional.ofNullable( recipe.assemble(moldItem) ).ifPresent(
                    stack -> {
                        mold.inventory.setStackInSlot(OUTPUT_SLOT, stack);
                        moldItem.drainIgnoringTemperature(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                    }
                );
            }
        }
    }

    public static final int MOLD_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOTS = 2;
    private static final Component NAME = Helpers.literal("");

    public MoldBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCCCBlockEntities.MOLD_TABLE.get(), pos, state, defaultInventory(SLOTS), NAME);
        sidedInventory.on(new PartialItemHandler(inventory).extract(MOLD_SLOT), Direction.UP);
        sidedInventory.on(new PartialItemHandler(inventory).extract(OUTPUT_SLOT), Direction.DOWN);
    }

    public ItemStackHandler getInventory()
    {
        return inventory;
    }

    public ItemStack getMoldStack()
    {
        return inventory.getStackInSlot(MOLD_SLOT);
    }

    public ItemStack getOutputStack()
    {
        return inventory.getStackInSlot(OUTPUT_SLOT);
    }

    public void setMoldStack(ItemStack stack)
    {
        inventory.setStackInSlot(MOLD_SLOT, stack);
    }

    public void setOutputStack(ItemStack stack)
    {
        inventory.setStackInSlot(OUTPUT_SLOT, stack);
    }

    public InteractionResult onRightClick(Player player)
    {
        final boolean interactWithMoldSlot = player.isShiftKeyDown() || inventory.getStackInSlot(MOLD_SLOT).isEmpty();

        if (interactWithMoldSlot)
        {
            final ItemStack heldItem = player.getMainHandItem();
            final boolean shouldExtract = !inventory.getStackInSlot(MOLD_SLOT).isEmpty();
            final boolean shouldInsert = !heldItem.isEmpty() && isItemValid(MOLD_SLOT, heldItem);

            if (shouldExtract)
            {
                // Swap items
                if (shouldInsert)
                {
                    final ItemStack extracted = inventory.extractItem(MOLD_SLOT, 1, false);
                    inventory.insertItem(MOLD_SLOT, heldItem.split(1), false);
                    if (!level.isClientSide)
                    {
                        ItemHandlerHelper.giveItemToPlayer(player, extracted, player.getInventory().selected);
                    }
                    markForBlockUpdate();
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                // Just extract
                if (!level.isClientSide)
                {
                    ItemHandlerHelper.giveItemToPlayer(player, inventory.extractItem(MOLD_SLOT, 1, false), player.getInventory().selected);
                }
                markForBlockUpdate();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            else if (shouldInsert)
            {
                inventory.insertItem(MOLD_SLOT, heldItem.split(1), false);
                markForBlockUpdate();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        else
        {
            final boolean shouldExtract = !inventory.getStackInSlot(OUTPUT_SLOT).isEmpty();
            if (shouldExtract)
            {
                if (!level.isClientSide)
                {
                    ItemHandlerHelper.giveItemToPlayer(player, inventory.extractItem(OUTPUT_SLOT, 1, false), player.getInventory().selected);
                }
                markForBlockUpdate();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        if (slot == MOLD_SLOT)
        {
            return stack.getCapability(Capabilities.FLUID_ITEM).isPresent() && stack.getCapability(HeatCapability.CAPABILITY).isPresent();
        }

        return true;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

}
