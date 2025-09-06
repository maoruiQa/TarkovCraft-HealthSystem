package tnt.tarkovcraft.medsystem.common;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import tnt.tarkovcraft.core.api.MovementStaminaComponent;
import tnt.tarkovcraft.core.api.event.EntityWeightUpdateEvent;
import tnt.tarkovcraft.core.api.event.StaminaEvent;
import tnt.tarkovcraft.core.common.attribute.AttributeSystem;
import tnt.tarkovcraft.core.common.energy.EnergySystem;
import tnt.tarkovcraft.core.common.skill.SkillSystem;
import tnt.tarkovcraft.core.common.statistic.StatisticTracker;
import tnt.tarkovcraft.core.util.context.Context;
import tnt.tarkovcraft.core.util.context.ContextImpl;
import tnt.tarkovcraft.core.util.context.ContextKeys;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.api.ArmorComponent;
import tnt.tarkovcraft.medsystem.api.heal.SideEffectHolder;
import tnt.tarkovcraft.medsystem.api.heal.SideEffectProcessor;
import tnt.tarkovcraft.medsystem.common.config.MedSystemConfig;
import tnt.tarkovcraft.medsystem.common.effect.MaxOverweightStatusEffect;
import tnt.tarkovcraft.medsystem.common.effect.OverweightStatusEffect;
import tnt.tarkovcraft.medsystem.common.effect.StatusEffectMap;
import tnt.tarkovcraft.medsystem.common.health.*;
import tnt.tarkovcraft.medsystem.common.health.math.DamageDistributor;
import tnt.tarkovcraft.medsystem.common.health.math.HitCalculator;
import tnt.tarkovcraft.medsystem.common.init.*;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MedicalSystemEventHandler {

    @SubscribeEvent
    private void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.isCanceled())
            return;
        if (entity instanceof LivingEntity livingEntity) {
            // Get the actual health container (not definition)
            if (HealthSystem.hasCustomHealth(livingEntity)) {
                HealthContainer container = HealthSystem.getHealthData(livingEntity);
                
                if (entity instanceof Player player) {
                    // Always ensure health multipliers are applied for all players
                    // This handles both new players and existing players consistently
                    updateHealthMultipliers(container, player);
                    
                    // Reset health to full for players on spawn if they have full vanilla health (respawn case)
                    if (player.getHealth() >= player.getMaxHealth()) {
                        container.getBodyPartStream().forEach(part -> {
                            part.setHealth(part.getMaxHealth());
                        });
                        // Clear any status effects that might have persisted
                        Context context = tnt.tarkovcraft.core.util.context.ContextImpl.of(
                            ContextKeys.LIVING_ENTITY, livingEntity,
                            MedicalSystemContextKeys.HEALTH_CONTAINER, container
                        );
                        container.getGlobalStatusEffects().removeAll(context);
                    }
                }
                
                HealthSystem.synchronizeEntity(livingEntity);
            }
        }
    }

    @SubscribeEvent
    private void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        float amount = event.getAmount();
        if (event.isCanceled())
            return;
            
        // Prevent downed players from healing
        if (entity instanceof Player player && HealthSystem.hasCustomHealth(player)) {
            HealthContainer container = HealthSystem.getHealthData(player);
            if (container.isPlayerDowned()) {
                // Cancel healing for downed players
                event.setCanceled(true);
                return;
            }
        }
            
        if (amount > 0.0F && HealthSystem.hasCustomHealth(entity)) {
            float leftover = entity.getData(MedSystemDataAttachments.HEALTH_CONTAINER).heal(entity, amount, null);
            if (leftover > 0.0F) {
                event.setAmount(amount - leftover);
            }
            HealthSystem.synchronizeEntity(entity);
        }
    }

    // Hitbox collision detection
    @SubscribeEvent
    private void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.isInvulnerable())
            return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity livingEntity))
            return;
        if (!HealthSystem.hasCustomHealth(livingEntity))
            return;

        HealthContainer container = entity.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        DamageSource source = event.getSource();
        HitCalculator hitCalculator = HealthSystem.getHitCalculator(livingEntity, source, container);
        List<HitResult> hits = hitCalculator.calculateHits(livingEntity, source, container);
        if (hits == null || hits.isEmpty()) {
            event.setInvulnerable(true);
        } else {
            DamageContext context = new DamageContext(livingEntity, source);
            context.setHits(hits);
            context.setHitCalculator(hitCalculator);
            context.setSideEffects(SideEffectHolder.fromDamage(source));
            container.setDamageContext(context);
        }
    }

    // Armor damaging
    @SubscribeEvent
    private void onArmorHit(ArmorHurtEvent event) {
        if (event.isCanceled())
            return;
        LivingEntity entity = event.getEntity();
        ArmorComponent component = HealthSystem.ARMOR.getComponent();
        if (!HealthSystem.hasCustomHealth(entity) || component.useVanillaArmorDamage())
            return;
        HealthContainer container = entity.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        DamageContext context = container.getDamageContext();
        Set<EquipmentSlot> hitSlots = new HashSet<>(context.getAffectedSlots());
        Set<EquipmentSlot> armorSlots = new HashSet<>(event.getArmorMap().keySet());
        Map<EquipmentSlot, ArmorHurtEvent.ArmorEntry> map = event.getArmorMap();
        float damageReductionMultiplier = AttributeSystem.getFloatValue(entity, MedSystemAttributes.ARMOR_DURABILITY, 1.0F);
        for (EquipmentSlot slot : armorSlots) {
            if (!hitSlots.contains(slot)) {
                map.remove(slot);
            } else {
                float damage = event.getNewDamage(slot);
                if (damage > 0 && damageReductionMultiplier != 1.0F) {
                    event.setNewDamage(slot, Math.max(damage * damageReductionMultiplier, 1.0F));
                }
            }
        }
    }

    // Entity armor damage recalculation
    @SubscribeEvent
    private void onLivingDamage(LivingIncomingDamageEvent event) {
        // calculate correct damage for armor and so on
        LivingEntity entity = event.getEntity();
        if (!HealthSystem.hasCustomHealth(entity))
            return;

        HealthContainer container = entity.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        DamageContext context = container.getDamageContext();
        List<HitResult> hits = context.getHits();
        // Hit hitbox groups
        EnumSet<BodyPartGroup> hitGroups = EnumSet.noneOf(BodyPartGroup.class);
        for (HitResult hit : hits) {
            BodyPart bodyPart = hit.bodyPart();
            BodyPartGroup group = bodyPart.getGroup();
            hitGroups.add(group);
        }
        ArmorComponent component = HealthSystem.ARMOR.getComponent();
        // Protected hitbox groups
        EnumSet<BodyPartGroup> protectedGroups = EnumSet.noneOf(BodyPartGroup.class);
        component.collectAffectedBodyPartsWithProtection(
                protectedGroups::add,
                entity,
                context
        );
        // remove not affected groups
        protectedGroups.removeIf(group -> !hitGroups.contains(group));
        // armor reduction calculation preparation

        Set<EquipmentSlot> protectedSlots = protectedGroups.stream()
                .flatMap(group -> group.getArmorSlots().stream())
                .collect(Collectors.toSet());

        context.setAffectedSlots(new ArrayList<>());
        float reduction = component.handleReductions(
                entity,
                context,
                protectedSlots,
                event::getAmount,
                event::setAmount,
                event::addReductionModifier
        );
        if (reduction > 0.0F) {
            SkillSystem.triggerAndSynchronize(MedSystemSkillEvents.ARMOR_USE, entity, reduction);
        }
    }

    // Entity damage application
    @SubscribeEvent
    private void onLivingApplyDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!HealthSystem.hasCustomHealth(entity))
            return;
        HealthContainer container = entity.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        DamageSource source = event.getSource();
        DamageContext context = container.getDamageContext();
        DamageDistributor damageDistributor = context.getDamageDistributor(container);
        Map<BodyPart, Float> distributedDamage = damageDistributor.distribute(context, container, event.getNewDamage());
        float totalDamage = distributedDamage.values().stream().reduce(0.0F, Float::sum);
        List<BodyPart> lostBodyParts = new ArrayList<>();
        SideEffectHolder sideEffects = context.getSideEffects();
        for (Map.Entry<BodyPart, Float> entry : distributedDamage.entrySet()) {
            container.hurt(context, entry.getValue(), entry.getKey(), lostBodyParts::add);
            if (sideEffects != null) {
                sideEffects.applyFromDamage(entity, source, container, entry.getKey());
            }
        }
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // Check if player is downed - if so, don't give skill experience
            boolean isDownedPlayer = false;
            if (entity instanceof Player player && HealthSystem.hasCustomHealth(player)) {
                HealthContainer healthContainer = HealthSystem.getHealthData(player);
                isDownedPlayer = healthContainer.isPlayerDowned();
            }
            
            if (!isDownedPlayer) {
                SkillSystem.triggerAndSynchronize(MedSystemSkillEvents.DAMAGE_TAKEN, entity, totalDamage);
            }
        }
        container.clearDamageContext();
        container.updateHealth(entity);
        float deathChance = lostBodyParts.isEmpty() ? 0.0F : MedicalSystem.getConfig().limbLossDeathCauseChance;
        if (deathChance > 0.0F) {
            float deathChanceMultiplier = AttributeSystem.getFloatValue(entity, MedSystemAttributes.LIMB_DEATH_CHANCE, 1.0F);
            deathChance *= (deathChanceMultiplier / lostBodyParts.size());
        }
        HealthSystem.synchronizeEntity(entity); // send status to client before death or further processing so that client knows which body part caused death
        if (container.shouldDie() || (deathChance > 0.0F && entity.getRandom().nextFloat() < deathChance)) {
            entity.setHealth(0.0F); // cannot use LivingEntity#die as that causes problems with xp/drops
        } else {
            // disable sprinting
            MovementStaminaComponent component = EnergySystem.MOVEMENT_STAMINA.getComponent();
            if (entity.isSprinting() && !component.canSprint(entity)) {
                entity.setSprinting(false);
            }
        }
    }

    @SubscribeEvent
    private void onWeightUpdate(EntityWeightUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        float factor = event.getOverweightFactor();
        if (!HealthSystem.hasCustomHealth(entity))
            return;
        HealthContainer container = HealthSystem.getHealthData(entity);
        StatusEffectMap effects = container.getGlobalStatusEffects();
        Context context = ContextImpl.of(
                ContextKeys.LIVING_ENTITY, entity,
                MedicalSystemContextKeys.HEALTH_CONTAINER, container
        );
        effects.removeMatching(MedSystemTags.StatusEffects.OVERWEIGHT, context);
        if (factor >= 1.5F) {
            effects.addEffect(new MaxOverweightStatusEffect());
        } else if (factor > 0.0F) {
            effects.addEffect(new OverweightStatusEffect());
        }
        HealthSystem.synchronizeEntity(entity);
    }

    @SubscribeEvent
    private void canSprint(StaminaEvent.CanSprint event) {
        LivingEntity entity = event.getEntity();
        MedSystemConfig config = MedicalSystem.getConfig();
        if (config.enableHitEffects && HealthSystem.isMovementRestricted(entity) && !HealthSystem.hasPainRelief(entity)) {
            event.setCanSprint(false);
        }
    }

    @SubscribeEvent
    private void onSprinted(StaminaEvent.AfterSprint event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        MedSystemConfig config = MedicalSystem.getConfig();
        long gametime = level.getGameTime();
        if (config.enableHitEffects && gametime % 20L == 0L && HealthSystem.isMovementRestricted(entity)) {
            RegistryAccess access = entity.registryAccess();
            DamageSource source = new DamageSource(MedSystemDamageTypes.of(access, MedSystemDamageTypes.BROKEN_LEG));
            entity.hurt(source, 0.25F);
        }
    }

    @SubscribeEvent
    private void afterJump(StaminaEvent.AfterJump event) {
        LivingEntity entity = event.getEntity();
        MedSystemConfig config = MedicalSystem.getConfig();
        if (config.enableHitEffects && HealthSystem.isMovementRestricted(entity)) {
            RegistryAccess access = entity.registryAccess();
            DamageSource source = new DamageSource(MedSystemDamageTypes.of(access, MedSystemDamageTypes.BROKEN_LEG));
            entity.hurt(source, 0.50F);
        }
    }

    @SubscribeEvent
    private void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled())
            return;
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        Entity killer = source.getEntity();
        if (HealthSystem.hasCustomHealth(entity)) {
            HealthContainer container = HealthSystem.getHealthData(entity);
            if (killer != null) {
                boolean headshot = source.is(DamageTypeTags.IS_PROJECTILE) && container.getBodyPartStream().anyMatch(part -> part.getGroup() == BodyPartGroup.HEAD && part.isDead());
                if (headshot) {
                    StatisticTracker.incrementOptional(killer, MedSystemStats.HEADSHOTS);
                    if (entity.getType() == EntityType.PLAYER) {
                        StatisticTracker.increment(killer, MedSystemStats.PLAYER_HEADSHOTS);
                    }
                }
            }
            container.invalidate();
        }
    }

    @SubscribeEvent
    private void addItemstackTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item.TooltipContext context = event.getContext();
        List<Component> tooltip = event.getToolTip();
        TooltipFlag flag = event.getFlags();
        Consumer<Component> adder = tooltip::add;

        stack.addToTooltip(MedSystemItemComponents.HEAL_ATTRIBUTES, context, adder, flag);
        stack.addToTooltip(MedSystemItemComponents.SIDE_EFFECTS, context, adder, flag);
    }

    @SubscribeEvent
    private void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getItem();
        LivingEntity entity = event.getEntity();
        if (!HealthSystem.hasCustomHealth(entity))
            return;
        if (stack.has(MedSystemItemComponents.SIDE_EFFECTS) && !(stack.getItem() instanceof SideEffectProcessor)) {
            SideEffectHolder holder = stack.get(MedSystemItemComponents.SIDE_EFFECTS);
            HealthContainer container = HealthSystem.getHealthData(entity);
            String targetLimb = stack.get(MedSystemItemComponents.SELECTED_BODY_PART);
            BodyPart part = container.getBodyPart(targetLimb);
            holder.apply(entity, container, part);
        }
    }

    // Prevent downed players from using items
    @SubscribeEvent
    private void onPlayerUseItem(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player))
            return;
        
        if (!HealthSystem.hasCustomHealth(player))
            return;
            
        HealthContainer container = HealthSystem.getHealthData(player);
        if (container.isPlayerDowned()) {
            event.setCanceled(true);
        }
    }

    // Prevent downed players from interacting with blocks/items
    @SubscribeEvent
    private void onPlayerInteractRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        
        if (!HealthSystem.hasCustomHealth(player))
            return;
            
        HealthContainer container = HealthSystem.getHealthData(player);
        if (container.isPlayerDowned()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onPlayerInteractRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        
        if (!HealthSystem.hasCustomHealth(player))
            return;
            
        HealthContainer container = HealthSystem.getHealthData(player);
        if (container.isPlayerDowned()) {
            event.setCanceled(true);
        }
    }

    // Prevent downed players from attacking
    @SubscribeEvent
    private void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        
        if (!HealthSystem.hasCustomHealth(player))
            return;
            
        HealthContainer container = HealthSystem.getHealthData(player);
        if (container.isPlayerDowned()) {
            event.setCanceled(true);
        }
    }

    // Make monsters ignore downed players and handle give up logic
    @SubscribeEvent
    private void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        
        if (!HealthSystem.hasCustomHealth(player))
            return;
            
        HealthContainer container = HealthSystem.getHealthData(player);
        if (container.isPlayerDowned()) {
            // Force swimming pose every tick on server side
            if (!player.level().isClientSide()) {
                player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);
            }
            
            // Prevent jumping by aggressively canceling upward movement every tick
            if (!player.level().isClientSide()) {
                Vec3 motion = player.getDeltaMovement();
                // Cancel any upward movement completely to prevent jumping
                if (motion.y > 0.0) {
                    player.setDeltaMovement(motion.x, 0.0, motion.z);
                }
                // Also prevent any jumping by resetting the player's on-ground state
                if (!player.onGround()) {
                    // Force player back to ground level if they somehow get airborne
                    player.setPos(player.getX(), player.getY() - 0.1, player.getZ());
                }
            }
            
            // Handle give up logic on client side
            if (player.level().isClientSide()) {
                tnt.tarkovcraft.medsystem.common.effect.DownedPlayerStatusEffect downedEffect = container.getDownedEffect();
                if (downedEffect != null) {
                    // Check if R key is being held (using GLFW key code for R)
                    boolean isRKeyHeld = false;
                    try {
                        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                        long window = minecraft.getWindow().getWindow();
                        isRKeyHeld = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_R) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                    } catch (Exception e) {
                        // Fallback if key detection fails
                        isRKeyHeld = false;
                    }
                    
                    downedEffect.tickGiveUp(isRKeyHeld);
                    
                    // Check if player wants to give up - simplified approach
                    if (downedEffect.shouldGiveUp()) {
                        // Reset the give up progress to prevent spam
                        downedEffect.setGiveUpProgress(0);
                        // The death timer will handle killing the player when it reaches 0
                    }
                }
            }
            
            // Make monsters completely ignore downed players
            if (!player.level().isClientSide()) {
                // More aggressive approach - check all nearby monsters every tick
                player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(64.0))
                    .stream()
                    .filter(monster -> monster.getTarget() == player)
                    .forEach(monster -> {
                        // Completely remove the player as target
                        monster.setTarget(null);
                        // Stop navigation
                        monster.getNavigation().stop();
                        // Clear last hurt by mob to prevent revenge
                        monster.setLastHurtByMob(null);
                        // Force the monster to forget about the player
                        if (monster.getBrain() != null) {
                            // Clear any memory related to the player if using brain-based AI
                            monster.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
                            monster.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY);
                            monster.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY);
                        }
                    });
            }
        }
    }
    
    
    // Prevent monsters from targeting downed players in the first place
    @SubscribeEvent
    private void onLivingSetTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player))
            return;
            
        if (!HealthSystem.hasCustomHealth(player))
            return;
            
        HealthContainer container = HealthSystem.getHealthData(player);
        if (container.isPlayerDowned()) {
            // Cancel the targeting event completely
            event.setCanceled(true);
        }
    }
    
    private static void updateHealthMultipliers(HealthContainer container, Player player) {
        MedSystemConfig config = MedicalSystem.getConfig();
        
        container.getBodyPartStream().forEach(part -> {
            // Check if this part needs its max health updated based on current multipliers
            float expectedMaxHealth = part.getOriginalMaxHealth();
            
            switch (part.getGroup()) {
                case HEAD:
                    expectedMaxHealth *= config.headHealthMultiplier;
                    break;
                case TORSO:
                    expectedMaxHealth *= config.chestHealthMultiplier;
                    break;
                case ARM:
                    expectedMaxHealth *= config.armHealthMultiplier;
                    break;
                case LEG:
                    expectedMaxHealth *= config.legHealthMultiplier;
                    break;
                default:
                    // No multiplier for other parts
                    break;
            }
            
            // Only update if the max health doesn't match expected (avoid unnecessary updates)
            if (Math.abs(part.getMaxHealth() - expectedMaxHealth) > 0.01f) {
                float currentHealthPercent = part.getHealth() / part.getMaxHealth();
                part.setMaxHealth(expectedMaxHealth);
                // Maintain the same health percentage
                part.setHealth(expectedMaxHealth * currentHealthPercent);
            }
        });
        
        // Update player's vanilla health to match container
        container.updateHealth(player);
    }
}
