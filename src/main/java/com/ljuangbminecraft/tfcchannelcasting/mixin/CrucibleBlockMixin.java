package com.ljuangbminecraft.tfcchannelcasting.mixin;

import com.ljuangbminecraft.tfcchannelcasting.common.blocks.ChannelBlock;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.CrucibleBlock;
import net.dries007.tfc.common.blocks.devices.DeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Used to add states to render a connection between channels and crucibles
@Mixin(CrucibleBlock.class)
public abstract class CrucibleBlockMixin extends DeviceBlock 
{
    @Shadow @Final
    private static VoxelShape SHAPE;

    public CrucibleBlockMixin(ExtendedProperties properties, InventoryRemoveBehavior removeBehavior) {
        super(properties, removeBehavior);
    }

    private static final BooleanProperty NORTH = PipeBlock.NORTH;
    private static final BooleanProperty EAST = PipeBlock.EAST;
    private static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    private static final BooleanProperty WEST = PipeBlock.WEST;

    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static
    {
        final VoxelShape base  = SHAPE;

        final VoxelShape east  = box(15.0D, 0.0D, 5.0D,  16.0D, 5.0D, 11.0D);
        final VoxelShape south = box(5.0D,  0.0D, 15.0D, 11.0D, 5.0D, 16.0D);
        final VoxelShape west  = box(0.0D,  0.0D, 5.0D,  1.0D,  5.0D, 11.0D);
        final VoxelShape north = box(5.0D,  0.0D, 0.0D,  11.0D, 5.0D, 1.0D);

        final VoxelShape[] directions = new VoxelShape[] {south, west, north, east};

        for (int i = 0; i < SHAPES.length; i++)
        {
            VoxelShape shape = base;
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                if (((i >> direction.get2DDataValue()) & 1) == 1)
                {
                    shape = Shapes.or(shape, directions[direction.get2DDataValue()]);
                }
            }
            SHAPES[i] = shape;
        }
    }

    private static BlockState updateConnectedSides(LevelAccessor level, BlockPos pos, BlockState state)
    {
        for (final Direction direction : Direction.Plane.HORIZONTAL)
        {
            final BlockPos adjacentPos = pos.relative(direction);
            final BlockState adjacentState = level.getBlockState(adjacentPos);
            final Block adjancentBlock = adjacentState.getBlock();
            final boolean isAdjacentConnectable = adjancentBlock instanceof ChannelBlock;
            state = state.setValue(DirectionPropertyBlock.getProperty(direction), isAdjacentConnectable);
        }

        return state;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ChannelBlockConstructor(ExtendedProperties properties, CallbackInfo ci) 
    {
        this.registerDefaultState(this.defaultBlockState().setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(NORTH, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[(state.getValue(NORTH) ? 1 << Direction.NORTH.get2DDataValue() : 0) |
            (state.getValue(EAST) ? 1 << Direction.EAST.get2DDataValue() : 0) |
            (state.getValue(SOUTH) ? 1 << Direction.SOUTH.get2DDataValue() : 0) |
            (state.getValue(WEST) ? 1 << Direction.WEST.get2DDataValue() : 0)];
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacentState, LevelAccessor level, BlockPos pos, BlockPos adjacentPos)
    {
        return updateConnectedSides(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(EAST, SOUTH, WEST, NORTH);
    }
    
}
