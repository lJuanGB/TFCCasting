package com.ljuangbminecraft.tfcchannelcasting.common.blockentities;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.ljuangbminecraft.tfcchannelcasting.common.TFCCCTags;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.MoldLike;
import net.dries007.tfc.common.capabilities.PartialFluidHandler;
import net.dries007.tfc.common.capabilities.PartialItemHandler;
import net.dries007.tfc.common.capabilities.SidedHandler;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.heat.IHeat;
import net.dries007.tfc.common.capabilities.heat.IHeatBlock;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.ingredients.FluidStackIngredient;
import net.dries007.tfc.common.recipes.ingredients.ItemStackIngredient;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class MoldBlockEntity extends TickableInventoryBlockEntity<MoldBlockEntity.MoldBlockInventory>
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, MoldBlockEntity mold)
    {
        // If output is already present, do not move or try to draw liquids
        if (!mold.getOutputStack().isEmpty())
        {
            if (mold.hasSource()) mold.finishFlow();
            return;
        }

        // Try to draw from crucible
        if (mold.hasSource() && level.getGameTime() % 2 == 0) // Draw at half the speed
        {
            level.getBlockEntity(mold.sourcePosition.get(), TFCBlockEntities.CRUCIBLE.get()).ifPresent(
            crucible -> {
                // This also ensures that the crucible's metal is the same as the one we
                // were expecting (mold.fluid) and rejects changes.
                // This ensures that we are not drawing two different fluids in the same "flow"
                Optional<IFluidHandler> fHandler = getFluidHandlerIfAppropriate(crucible, mold.fluid);

                // If the chain of channels was broken also finish the flow
                if (fHandler.isEmpty() || mold.isLinkBroken())
                {
                    mold.finishFlow();
                    return;
                }
                
                final FluidStack outputDrop = fHandler.get().drain(1, IFluidHandler.FluidAction.SIMULATE);
                final FluidStack outputRemainder = Helpers.mergeOutputFluidIntoSlot(mold.getInventory(), outputDrop, crucible.getTemperature(), MoldBlockEntity.MOLD_SLOT);
                if (outputRemainder.isEmpty())
                {
                    // Remainder was emptied, so do the extraction for real
                    fHandler.get().drain(1, IFluidHandler.FluidAction.EXECUTE);
                    crucible.markForSync();
                    mold.markForBlockUpdate(); // Maybe overkill update, but markForSync will not work for rendering
                }
                else
                {
                    // Could not fill any longer, finish the flow
                    mold.finishFlow();
                }
            }
            );
        }

        // Move results from mold item to output stack
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

    /***
     * Returns the crucible's IFluidHandler only if:
     *  - The crucible is not empty
     *  - The fluid in the crucible is the same as shouldBeFluid, if given
     *  - The metal is melted
     */
    public static Optional<IFluidHandler> getFluidHandlerIfAppropriate(CrucibleBlockEntity crucible, Optional<Fluid> shouldBeFluid)
    {
        IFluidHandler crucibleFluidHandler = crucible.getCapability(Capabilities.FLUID).resolve().get();
        IHeatBlock crucibleHeatHandler = crucible.getCapability(HeatCapability.BLOCK_CAPABILITY).resolve().get();

        Metal metal = Metal.get(crucibleFluidHandler.getFluidInTank(0).getFluid());

        if (   metal == null
        || (shouldBeFluid.isPresent() && (metal.getFluid().getRegistryName() != shouldBeFluid.get().getRegistryName()))
        || (metal.getMeltTemperature() > crucibleHeatHandler.getTemperature()))
        {
            return Optional.empty();
        }

        return Optional.of(crucibleFluidHandler);
    }

    /*** The position of the crucible where the flow is coming from 
     * 
     * Empty if the mold table does not have a flow currently
    */
    private Optional<BlockPos> sourcePosition = Optional.empty();

    /*** The direction where the flow is coming from, 
     * as well as the distance (for downwards flow) 
     * 
     * Empty if the channel does not have a flow currently.
    */
    private Optional<Pair<Direction, Byte>> flowSource = Optional.empty();

    /*** The fluid to expect from the crucible. Flow should finish
     * if the fluid from the crucible is different than this value. 
     * 
     * Empty if the mold table does not have a flow currently
    */
    private Optional<Fluid> fluid = Optional.empty();

    private final SidedHandler.Builder<IFluidHandler> sidedFluidInventory;
    private final SidedHandler.Builder<IHeatBlock> sidedHeatInventory;

    public static final int MOLD_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final Component NAME = Helpers.literal("Mold table");

    public MoldBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCCCBlockEntities.MOLD_TABLE.get(), pos, state, MoldBlockInventory::new, NAME);

        sidedFluidInventory = new SidedHandler.Builder<>(inventory);
        sidedHeatInventory = new SidedHandler.Builder<>(inventory);

        sidedFluidInventory.on(
            new PartialFluidHandler(inventory).insert(), // Allow input fluid from all sides and top
            Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP); 
        
        PartialItemHandler handler = new PartialItemHandler(inventory);
        sidedInventory.on(handler.insert(MOLD_SLOT), Direction.UP)
                      .on(handler.extract(OUTPUT_SLOT), Direction.DOWN);
    }

    public boolean hasSource()
    {
        return sourcePosition.isPresent();
    }

    public void finishFlow()
    {
        flowSource.ifPresent(
            fSource -> {
                level.getBlockEntity(
                    worldPosition.relative(fSource.getLeft(), fSource.getRight()), TFCCCBlockEntities.CHANNEL.get()
                ).ifPresent(
                    channel -> channel.notifyBrokenLink(1)
                );
                
                markForBlockUpdate();
                sourcePosition = Optional.empty();
                fluid = Optional.empty();
                flowSource = Optional.empty();
            }
        );
    }

    public void setSource(BlockPos sourcePos, Fluid fluid, Pair<Direction, Byte> flowSource)
    {
        this.sourcePosition = Optional.of(sourcePos);
        this.fluid = Optional.of(fluid);
        this.flowSource = Optional.of(flowSource);
    }

    public boolean isLinkBroken()
    {
        Optional<ChannelBlockEntity> bEntity = level.getBlockEntity(
            worldPosition.relative(flowSource.get().getLeft(), flowSource.get().getRight()), 
            TFCCCBlockEntities.CHANNEL.get()
        );

        if (bEntity.isEmpty()) return true;

        for (byte i = 1; i < flowSource.get().getRight(); i++)
        {
            BlockPos rel = worldPosition.relative(flowSource.get().getLeft(), i);
            if (!level.getBlockState(rel).isAir()) {
                return true;
            }
        }

        return bEntity.get().isLinkBroken();
    }

    public Fluid getSourceFluid()
    {
        return fluid.get();
    }

    public Pair<Direction, Byte> getFlowSource() {
        return flowSource.get();
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
            return Helpers.isItem(stack, TFCCCTags.Items.ACCEPTED_IN_MOLD_TABLES);
        }

        return true;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }
    
    @Override
    public void loadAdditional(CompoundTag nbt)
    {
        if (nbt.contains("sourcePosition"))
        {
            sourcePosition = Optional.of(BlockPos.of( nbt.getLong("sourcePosition") ));
            flowSource = Optional.of(
                Pair.of(
                    Direction.values()[nbt.getByte("flowSource")], 
                    nbt.contains("flowSourceDistance") ? nbt.getByte("flowSourceDistance") : 1
                )
            );
            fluid = Optional.of(ForgeRegistries.FLUIDS.getValue( new ResourceLocation(nbt.getString("fluid")) ));
        }
        else
        {
            sourcePosition = Optional.empty();
            flowSource = Optional.empty();
            fluid = Optional.empty();
        }
        super.loadAdditional(nbt);
    }

    @Override
    public void saveAdditional(CompoundTag nbt)
    {
        if (hasSource())
        {
            nbt.putLong("sourcePosition", sourcePosition.get().asLong());
            nbt.putByte("flowSource", (byte) flowSource.get().getLeft().ordinal());
            nbt.putByte("flowSourceDistance", flowSource.get().getRight());
            nbt.putString("fluid", fluid.get().getRegistryName().toString());
        }
        super.saveAdditional(nbt);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
    {
        if (cap == Capabilities.FLUID)
        {
            return sidedFluidInventory.getSidedHandler(side).cast();
        }
        if (cap == HeatCapability.BLOCK_CAPABILITY)
        {
            return sidedHeatInventory.getSidedHandler(side).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCapabilities()
    {
        sidedInventory.invalidate();
        sidedFluidInventory.invalidate();
        sidedHeatInventory.invalidate();
    }

    @Override
    public AABB getRenderBoundingBox()
    {
        if (this.flowSource.isPresent())
        {
            if (this.flowSource.get().getLeft() == Direction.UP)
            {
                return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 1+this.flowSource.get().getRight(), 2));
            }
            else
            {
                return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
            }
        }
        else
        {
            return super.getRenderBoundingBox();
        }
    }
    
    // This inventory delegates the fluid and heat handler to the
    // item in the mold stack, as long as this item has the
    // corresponding capabilities (i.e. a mold item is present).
    // If it does not have them, it implements some default behaviour
    // for every method.
    // Moreover, it adds some custom behaviour for fill
    static class MoldBlockInventory extends ItemStackHandler implements IFluidHandler, IHeatBlock
    {
        private final MoldBlockEntity moldTable;

        MoldBlockInventory(InventoryBlockEntity<?> entity)
        {
            super(2);
            moldTable = (MoldBlockEntity) entity;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            moldTable.markForBlockUpdate();
        }

        private Optional<IFluidHandler> getMoldFluidHandler()
        {
            return moldTable.getMoldStack().getCapability(Capabilities.FLUID).map(t -> t);
        }

        private Optional<IHeat> getMoldHeatHandler()
        {
            return moldTable.getMoldStack().getCapability(HeatCapability.CAPABILITY).map(t -> t);
        }

        @Override
        public float getTemperature() {
            return getMoldHeatHandler().map(h -> h.getTemperature()).orElse(0f);
        }

        @Override
        public void setTemperature(float temp) {
            getMoldHeatHandler().ifPresent(h -> h.setTemperature(temp));
        }

        @Override
        public int getTanks() {
            return getMoldFluidHandler().map(h -> h.getTanks()).orElse(0);
        }

        @Override
        @Nonnull
        public FluidStack getFluidInTank(int tank) {
            return getMoldFluidHandler().map(h -> h.getFluidInTank(tank)).orElse(FluidStack.EMPTY.copy());
        }

        @Override
        public int getTankCapacity(int tank) {
            return getMoldFluidHandler().map(h -> h.getTankCapacity(tank)).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return getMoldFluidHandler().map(h -> h.isFluidValid(tank, stack)).orElse(false);
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) 
        {
            // if mold or output are present, try to apply instantaneous barrel recipes
            // Essentially this allows cooling down the molds,
            // although it accepts other recipes if added
            ItemStack stack;
            boolean usedMoldStack;

            if (!moldTable.getOutputStack().isEmpty())
            {
                stack = moldTable.getOutputStack();
                usedMoldStack = false;
            }
            else
            {
                stack = moldTable.getMoldStack();
                usedMoldStack = true;
            }

            Level level = moldTable.getLevel();

            if (!stack.isEmpty() && level != null)
            {
                final RecipeManager recipeManager = level.getRecipeManager();

                // If there is an instant barrel recipe with the mold stack
                // and the filled liquid, then this returns a Pair with the
                // result ItemStack and the amount of fluid that would be consumed
                Optional<Pair<ItemStack, Integer>> recipeResult = recipeManager
                .getAllRecipesFor(
                    TFCRecipeTypes.BARREL_INSTANT.get()).stream()
                .filter(
                    recipe -> recipe.getInputItem().test(stack) && recipe.getInputFluid().test(fluid)
                ).findFirst().map(
                    recipe -> 
                    {
                        ItemStackIngredient inputItem = recipe.getInputItem();
                        FluidStackIngredient inputFluid = recipe.getInputFluid();

                        // Calculate the multiplier in use for this recipe
                        int multiplier;
                        if (inputItem.count() == 0)
                        {
                            multiplier = fluid.getAmount() / inputFluid.amount();
                        }
                        else if (inputFluid.amount() == 0)
                        {
                            multiplier = stack.getCount() / inputItem.count();
                        }
                        else
                        {
                            multiplier = Math.min(fluid.getAmount() / inputFluid.amount(), stack.getCount() / inputItem.count());
                        }

                        // Compute output
                        final ItemStack outputItem = recipe.getOutputItem().getSingleStack(stack);
                        if (!outputItem.isEmpty())
                        {
                            outputItem.setCount( Math.min(outputItem.getMaxStackSize(), multiplier * outputItem.getCount() ) );
                        }

                        // Amount of fluid that would be consumed
                        return Pair.of(outputItem, multiplier * inputFluid.amount());
                    }
                );

                if (recipeResult.isPresent())
                {
                    if (action == FluidAction.EXECUTE)
                    {
                        if (usedMoldStack)
                        {
                            moldTable.setMoldStack(recipeResult.get().getLeft());
                        }
                        else
                        {
                            moldTable.setOutputStack(recipeResult.get().getLeft());
                        }
                    }
                    return recipeResult.get().getRight();
                }
            }

            return getMoldFluidHandler().map(h -> h.fill(fluid, action)).orElse(0);
        }

        @Override
        @Nonnull
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return getMoldFluidHandler().map(h -> h.drain(resource, action)).orElse(FluidStack.EMPTY.copy());
        }

        @Override
        @Nonnull
        public FluidStack drain(int maxDrain, FluidAction action) {
            return getMoldFluidHandler().map(h -> h.drain(maxDrain, action)).orElse(FluidStack.EMPTY.copy());
        }
    }
}
