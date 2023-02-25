package com.teamabnormals.caverns_and_chasms.common.item;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.teamabnormals.caverns_and_chasms.core.registry.CCSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Wearable;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TetherPotionItem extends PotionItem implements Wearable {

	public TetherPotionItem(Properties properties) {
		super(properties);
	}

	@Override
	public EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
	}

	@Override
	public void onArmorTick(ItemStack stack, Level world, Player player) {
		if (!world.isClientSide()) {
			for (MobEffectInstance instance : PotionUtils.getMobEffects(stack)) {
				if (!instance.getEffect().isInstantenous()) {
					MobEffectInstance instance1 = new MobEffectInstance(instance.getEffect(), 36000, instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon());
					player.addEffect(instance1);
				}
			}
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(itemstack);
		ItemStack itemstack1 = player.getItemBySlot(equipmentslot);
		if (itemstack1.isEmpty()) {
			player.setItemSlot(equipmentslot, itemstack.copy());
			if (!level.isClientSide()) {
				player.awardStat(Stats.ITEM_USED.get(this));
			}

			itemstack.setCount(0);
			return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
		} else {
			return InteractionResultHolder.fail(itemstack);
		}
	}

	@Nullable
	@Override
	public SoundEvent getEquipSound() {
		return CCSoundEvents.TETHER_POTION_EQUIP.get();
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		List<MobEffectInstance> list = PotionUtils.getMobEffects(stack);
		List<Pair<Attribute, AttributeModifier>> list1 = Lists.newArrayList();

		List<Component> instanttooltip = Lists.newArrayList();
		List<Component> continuoustooltip = Lists.newArrayList();

		if (list.isEmpty()) {
			continuoustooltip.add((Component.translatable("effect.none")).withStyle(ChatFormatting.GRAY));
		} else {
			for (MobEffectInstance mobeffectinstance : list) {
				MutableComponent mutablecomponent = Component.translatable(mobeffectinstance.getDescriptionId());
				MobEffect effect = mobeffectinstance.getEffect();
				Map<Attribute, AttributeModifier> map = effect.getAttributeModifiers();
				if (!map.isEmpty()) {
					for (Entry<Attribute, AttributeModifier> entry : map.entrySet()) {
						AttributeModifier attributemodifier = entry.getValue();
						AttributeModifier attributemodifier1 = new AttributeModifier(attributemodifier.getName(), effect.getAttributeModifierValue(mobeffectinstance.getAmplifier(), attributemodifier), attributemodifier.getOperation());
						list1.add(new Pair<>(entry.getKey(), attributemodifier1));
					}
				}

				if (mobeffectinstance.getAmplifier() > 0) {
					mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + mobeffectinstance.getAmplifier()));
				}

				if (effect.isInstantenous()) {
					instanttooltip.add(mutablecomponent.withStyle(effect.getCategory().getTooltipFormatting()));
				} else {
					mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, StringUtil.formatTickDuration(getTetherPotionDuration(mobeffectinstance.getDuration())));
					continuoustooltip.add(mutablecomponent.withStyle(effect.getCategory().getTooltipFormatting()));
				}
			}
		}

		tooltip.addAll(instanttooltip);
		if (!continuoustooltip.isEmpty()) {
			tooltip.add(Component.empty());
			tooltip.add((Component.translatable("item.modifiers." + EquipmentSlot.HEAD.getName())).withStyle(ChatFormatting.GRAY));
			tooltip.addAll(continuoustooltip);
		}

		if (!list1.isEmpty()) {
			tooltip.add(Component.empty());
			tooltip.add((Component.translatable("potion.whenDrank")).withStyle(ChatFormatting.DARK_PURPLE));

			for (Pair<Attribute, AttributeModifier> pair : list1) {
				AttributeModifier attributemodifier2 = pair.getSecond();
				double d0 = attributemodifier2.getAmount();
				double d1;
				if (attributemodifier2.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier2.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
					d1 = attributemodifier2.getAmount();
				} else {
					d1 = attributemodifier2.getAmount() * 100.0D;
				}

				if (d0 > 0.0D) {
					tooltip.add((Component.translatable("attribute.modifier.plus." + attributemodifier2.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(pair.getFirst().getDescriptionId()))).withStyle(ChatFormatting.BLUE));
				} else if (d0 < 0.0D) {
					d1 *= -1.0D;
					tooltip.add((Component.translatable("attribute.modifier.take." + attributemodifier2.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), Component.translatable(pair.getFirst().getDescriptionId()))).withStyle(ChatFormatting.RED));
				}
			}
		}
	}

	public static int getTetherPotionDuration(int originalDuration) {
		int duration = Math.round(10 - 1 / ((originalDuration / 20 + 200) * 0.0005F)) * 20;
		return Math.max(duration, 20);
	}
}