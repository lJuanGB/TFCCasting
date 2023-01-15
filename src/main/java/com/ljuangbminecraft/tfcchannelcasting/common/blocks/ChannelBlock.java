package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ljuangbminecraft.tfcchannelcasting.common.WithChannelFlows;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.dries007.tfc.common.blocks.ExtendedBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.CrucibleBlock;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChannelBlock extends ExtendedBlock
{
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream()
        .filter(facing -> facing.getKey() != Direction.UP).collect(Util.toMap());

    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;

    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static
    {
        final VoxelShape base  = box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);

        final VoxelShape east  = box(11.0D, 0.0D, 5.0D,  16.0D, 5.0D, 11.0D);
        final VoxelShape south = box(5.0D,  0.0D, 11.0D, 11.0D, 5.0D, 16.0D);
        final VoxelShape west  = box(0.0D,  0.0D, 5.0D,  5.0D,  5.0D, 11.0D);
        final VoxelShape north = box(5.0D,  0.0D, 0.0D,  11.0D, 5.0D, 10.0D);

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
        for (final Direction direction : Direction.values())
        {
            if (direction == Direction.UP) continue;

            final BlockPos adjacentPos = pos.relative(direction);
            final BlockState adjacentState = level.getBlockState(adjacentPos);
            final Block adjancentBlock = adjacentState.getBlock();
            final boolean isAdjacentConnectable = adjancentBlock instanceof ChannelBlock || adjancentBlock instanceof CrucibleBlock || adjancentBlock instanceof MoldBlock;
            state = state.setValue(DirectionPropertyBlock.getProperty(direction), isAdjacentConnectable);
        }

        return state;
    }

    public ChannelBlock(ExtendedProperties properties) 
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(NORTH, false).setValue(DOWN, false));
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
        pBuilder.add(EAST, SOUTH, WEST, NORTH, DOWN);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            for (Direction dir : Direction.values())
            {
                if (dir == Direction.UP) continue;

                Optional<CrucibleBlockEntity> crucible = level.getBlockEntity(pos.relative(dir), TFCBlockEntities.CRUCIBLE.get());
                if (crucible.isPresent())
                {
                    ((WithChannelFlows) crucible.get()).generateNewChannelFlow(dir.getOpposite());
                    return InteractionResult.PASS;
                }
            }
        }
        return InteractionResult.FAIL;
    }

    protected Map<BlockState, VoxelShape> makeShapes(VoxelShape middleShape, ImmutableList<BlockState> possibleStates)
    {
        final ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
        for (BlockState state : possibleStates)
        {
            VoxelShape shape = middleShape;
            for (Direction d : Direction.Plane.HORIZONTAL)
            {
                if (state.getValue(PROPERTY_BY_DIRECTION.get(d)))
                {
                    VoxelShape joinShape = switch (d)
                        {
                            case NORTH -> box(5.0D, 10.0D, 0.0D, 11.0D, 16.0D, 10.0D);
                            case SOUTH -> box(5.0D, 10.0D, 11.0D, 11.0D, 16.0D, 16.0D);
                            case EAST -> box(11.0D, 10.0D, 5.0D, 16.0D, 16.0D, 11.0D);
                            case WEST -> box(0.0D, 10.0D, 5.0D, 5.0D, 16.0D, 11.0D);
                            default -> Shapes.empty();
                        };
                    shape = Shapes.or(shape, joinShape);
                }
            }
            builder.put(state, shape);
        }
        return builder.build();
    }
}
