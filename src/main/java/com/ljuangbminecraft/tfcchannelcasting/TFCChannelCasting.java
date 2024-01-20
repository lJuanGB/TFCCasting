package com.ljuangbminecraft.tfcchannelcasting;

import org.slf4j.Logger;

import com.ljuangbminecraft.tfcchannelcasting.client.ClientEventHandler;
import com.ljuangbminecraft.tfcchannelcasting.common.TFCCCTags;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.ChannelBlock;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.MoldBlock;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCBlocks;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.TFCCCFluids;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;
import com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs.ModifyConditions;
import com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs.TFCCCItemStackModifiers;
import com.ljuangbminecraft.tfcchannelcasting.config.TFCCCConfig;
import com.ljuangbminecraft.tfcchannelcasting.common.items.ChocolateSweetItem;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCFoodTraits;
import com.mojang.logging.LogUtils;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.util.InteractionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;

@Mod(TFCChannelCasting.MOD_ID)
public class TFCChannelCasting {
        // Directly reference a slf4j logger
        public static final String MOD_ID = "tfcchannelcasting";
        public static final Logger LOGGER = LogUtils.getLogger();

        public TFCChannelCasting() {
                final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

                TFCCCBlocks.BLOCKS.register(bus);
                TFCCCBlockEntities.BLOCK_ENTITIES.register(bus);
                TFCCCItems.ITEMS.register(bus);
                TFCCCFluids.FLUIDS.register(bus);

                TFCCCConfig.init();

                bus.addListener(this::setup);

                if (FMLEnvironment.dist == Dist.CLIENT) {
                        ClientEventHandler.init();
                }
        }

        public void setup(FMLCommonSetupEvent event) {
                TFCCCItemStackModifiers.registerItemStackModifierTypes();
                ModifyConditions.registerItemStackModifierTypes();

                // Vanilla registries are not thread safe
                event.enqueueWork(() -> {
                        TFCChannelCasting.registerInteractions();
                        TFCCCFoodTraits.init();
                        ChocolateSweetItem.registerTraitEffects();
                });
                registerPatchouliMultiBlock();
        }

        // Handle shift-right click on molds
        public static void registerInteractions() {
                InteractionManager.register(
                                Ingredient.of(TFCCCTags.Items.ACCEPTED_IN_MOLD_TABLES),
                                false,
                                (stack, context) -> {
                                        final Player player = context.getPlayer();
                                        if (player != null) {
                                                final Level level = context.getLevel();
                                                final BlockPos posClicked = context.getClickedPos();
                                                level.getBlockEntity(posClicked, TFCCCBlockEntities.MOLD_TABLE.get())
                                                                .ifPresent(
                                                                                mold -> mold.onRightClick(player));
                                        }
                                        return InteractionResult.SUCCESS;
                                });
        }

        public void registerPatchouliMultiBlock() {
                final PatchouliAPI.IPatchouliAPI api = PatchouliAPI.get();

                // ^ W
                // |
                // +-> S

                final IMultiblock multiblock = api.makeMultiblock(new String[][] {
                                { "     ",
                                                "   R ",
                                                "   | ",
                                                " S-+c",
                                                "     "
                                },
                                { "  XXX",
                                                "  XFX",
                                                "  0XX",
                                                "CNXXX",
                                                "  XXX"
                                },
                                {
                                                "     ",
                                                "     ",
                                                "XXX  ",
                                                "XXX  ",
                                                "XXX  "
                                },
                },
                                'R',
                                api.stateMatcher(TFCBlocks.CRUCIBLE.get().defaultBlockState().setValue(PipeBlock.EAST,
                                                true)),
                                'F', api.looseBlockMatcher(TFCBlocks.CHARCOAL_FORGE.get()),
                                ' ', api.airMatcher(),
                                'X',
                                api.looseBlockMatcher(TFCBlocks.ROCK_BLOCKS.get(Rock.GRANITE).get(Rock.BlockType.BRICKS)
                                                .get()),
                                '0',
                                api.looseBlockMatcher(TFCBlocks.ROCK_BLOCKS.get(Rock.GRANITE).get(Rock.BlockType.BRICKS)
                                                .get()),
                                '|',
                                api.stateMatcher(TFCCCBlocks.CHANNEL.get().defaultBlockState()
                                                .setValue(ChannelBlock.WEST, true)
                                                .setValue(ChannelBlock.EAST, true)),
                                '-',
                                api.stateMatcher(TFCCCBlocks.CHANNEL.get().defaultBlockState()
                                                .setValue(ChannelBlock.SOUTH, true)
                                                .setValue(ChannelBlock.NORTH, true)),
                                '+',
                                api.stateMatcher(TFCCCBlocks.CHANNEL.get().defaultBlockState()
                                                .setValue(ChannelBlock.SOUTH, true)
                                                .setValue(ChannelBlock.NORTH, true).setValue(ChannelBlock.WEST, true)),
                                'S',
                                api.stateMatcher(TFCCCBlocks.CHANNEL.get().defaultBlockState()
                                                .setValue(ChannelBlock.SOUTH, true)),
                                'N',
                                api.stateMatcher(TFCCCBlocks.CHANNEL.get().defaultBlockState()
                                                .setValue(ChannelBlock.NORTH, true)),
                                'C',
                                api.stateMatcher(TFCCCBlocks.MOLD_TABLE.get().defaultBlockState()
                                                .setValue(MoldBlock.SOUTH, true)),
                                'c',
                                api.stateMatcher(TFCCCBlocks.MOLD_TABLE.get().defaultBlockState()
                                                .setValue(MoldBlock.NORTH, true)));

                api.registerMultiblock(new ResourceLocation("tfcchannelcasting", "example"), multiblock);
        }
}
