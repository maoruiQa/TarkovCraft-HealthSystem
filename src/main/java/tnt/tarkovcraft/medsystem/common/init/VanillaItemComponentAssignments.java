package tnt.tarkovcraft.medsystem.common.init;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import tnt.tarkovcraft.core.common.data.duration.Duration;
import tnt.tarkovcraft.core.common.data.duration.TickValue;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.config.MedSystemConfig;
import tnt.tarkovcraft.medsystem.api.heal.SideEffectHolder;

import java.util.function.BiConsumer;

public final class VanillaItemComponentAssignments {

    public static void adjustItemData(BiConsumer<ItemLike, SideEffectHolder> registration) {
        MedicalSystem.LOGGER.debug(MedicalSystem.MARKER, "Applying hit effects to vanilla items");
        TickValue effectDuration = Duration.minutes(2);
        MedSystemConfig cfg = MedicalSystem.getConfig();
        SideEffectHolder swords = SideEffectHolder.builder()
                .title(SideEffectHolder.ITEM_TITLE)
                .sideEffect(cfg.swordLightBleedChance, effectDuration, MedSystemStatusEffects.LIGHT_BLEED)
                .sideEffect(cfg.swordHeavyBleedChance, effectDuration, MedSystemStatusEffects.HEAVY_BLEED)
                .build();
        registration.accept(Items.WOODEN_SWORD, swords);
        registration.accept(Items.STONE_SWORD, swords);
        registration.accept(Items.IRON_SWORD, swords);
        registration.accept(Items.GOLDEN_SWORD, swords);
        registration.accept(Items.DIAMOND_SWORD, swords);
        registration.accept(Items.NETHERITE_SWORD, swords);

        SideEffectHolder axes = SideEffectHolder.builder()
                .title(SideEffectHolder.ITEM_TITLE)
                .sideEffect(cfg.axeLightBleedChance, effectDuration, MedSystemStatusEffects.LIGHT_BLEED)
                .sideEffect(cfg.axeHeavyBleedChance, effectDuration, MedSystemStatusEffects.HEAVY_BLEED)
                .sideEffect(cfg.axeFractureChance, effectDuration, MedSystemStatusEffects.FRACTURE)
                .build();
        registration.accept(Items.WOODEN_AXE, axes);
        registration.accept(Items.STONE_AXE, axes);
        registration.accept(Items.IRON_AXE, axes);
        registration.accept(Items.GOLDEN_AXE, axes);
        registration.accept(Items.DIAMOND_AXE, axes);
        registration.accept(Items.NETHERITE_AXE, axes);

        SideEffectHolder blunt = SideEffectHolder.builder()
                .title(SideEffectHolder.ITEM_TITLE)
                .infiniteSideEffect(cfg.bluntFractureChance, MedSystemStatusEffects.FRACTURE)
                .sideEffect(cfg.bluntLightBleedChance, effectDuration, MedSystemStatusEffects.LIGHT_BLEED)
                .build();
        registration.accept(Items.WOODEN_SHOVEL, blunt);
        registration.accept(Items.STONE_SHOVEL, blunt);
        registration.accept(Items.IRON_SHOVEL, blunt);
        registration.accept(Items.GOLDEN_SHOVEL, blunt);
        registration.accept(Items.DIAMOND_SHOVEL, blunt);
        registration.accept(Items.NETHERITE_SHOVEL, blunt);
        registration.accept(Items.WOODEN_PICKAXE, blunt);
        registration.accept(Items.STONE_PICKAXE, blunt);
        registration.accept(Items.IRON_PICKAXE, blunt);
        registration.accept(Items.GOLDEN_PICKAXE, blunt);
        registration.accept(Items.DIAMOND_PICKAXE, blunt);
        registration.accept(Items.NETHERITE_PICKAXE, blunt);
        registration.accept(Items.WOODEN_HOE, blunt);
        registration.accept(Items.STONE_HOE, blunt);
        registration.accept(Items.IRON_HOE, blunt);
        registration.accept(Items.GOLDEN_HOE, blunt);
        registration.accept(Items.DIAMOND_HOE, blunt);
        registration.accept(Items.NETHERITE_HOE, blunt);
        registration.accept(Items.MACE, blunt);
    }
}
