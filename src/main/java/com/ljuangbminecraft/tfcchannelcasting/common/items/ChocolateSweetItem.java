/*
    package com.ljuangbminecraft.tfcchannelcasting.common.items;

    import net.dries007.tfc.common.capabilities.food.FoodCapability;
    import net.dries007.tfc.common.capabilities.food.FoodHandler;
    import net.dries007.tfc.common.capabilities.food.FoodTrait;
    import net.minecraft.nbt.CompoundTag;
    import net.minecraft.world.effect.MobEffectInstance;
    import net.minecraft.world.effect.MobEffects;
    import net.minecraft.world.entity.LivingEntity;
    import net.minecraft.world.item.Item;
    import net.minecraft.world.item.ItemStack;
    import net.minecraft.world.level.Level;
    import net.minecraftforge.common.capabilities.ICapabilityProvider;

    import javax.annotation.Nullable;
    import java.util.HashMap;
    import java.util.Map;

    public class ChocolateSweetItem extends Item {
        public static Map<FoodTrait, MobEffectInstance> TRAIT_EFFECTS = new HashMap<>();

        public static void registerTraitEffects() {
            TRAIT_EFFECTS.put(TFCCCFoodTraits.FILLED_WITH_SWEET_LIQUOR,
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3200, 0));
            TRAIT_EFFECTS.put(TFCCCFoodTraits.FILLED_WITH_STRONG_LIQUOR,
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3200, 0));
            TRAIT_EFFECTS.put(TFCCCFoodTraits.FILLED_WITH_WHISKEY,
                    new MobEffectInstance(MobEffects.DIG_SPEED, 3200, 0));
        }

        public ChocolateSweetItem(Properties properties) {
            super(properties);
        }

        @Nullable
        @Override
        public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
            return new FoodHandler.Dynamic();
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            stack.getCapability(FoodCapability.CAPABILITY).ifPresent(
                    cap -> {
                        for (FoodTrait trait : cap.getTraits()) {
                            if (TRAIT_EFFECTS.containsKey(trait)) {
                                entity.addEffect(TRAIT_EFFECTS.get(trait));
                            }
                        }
                    });

            return super.finishUsingItem(stack, level, entity);
        }
    }
    */