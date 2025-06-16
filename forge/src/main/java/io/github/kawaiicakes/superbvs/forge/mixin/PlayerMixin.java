package io.github.kawaiicakes.superbvs.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(Player.class)
public abstract class PlayerMixin extends Entity {
    public PlayerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;minmax(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/AABB;"
            )
    )
    public AABB fixRidingInteractions(AABB instance, AABB arg, Operation<AABB> original) {
        // second AABB arg may be the position of a vehicle entity on a ship. This causes the console spam
        AABB nonSus = arg;

        LogManager.getLogger().warn("This injected properly!");
        if (VSGameUtilsKt.isBlockInShipyard(this.level(), arg.getCenter())) {
            nonSus = VSGameUtilsKt.transformAabbToWorld(this.level(), arg);
            LogManager.getLogger().warn("Sus AABB transformed to {}", nonSus);
        }

        return original.call(instance, nonSus);
    }
}
