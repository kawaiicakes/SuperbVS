package io.github.kawaiicakes.superbvs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;

import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(RaycastUtilsKt.class)
public interface RaycastUtilsKtInvoker {
    @Invoker("clip")
    static <T> T clip(
            Vec3 realStart, Vec3 realEnd, ClipContext clipContext,
            BiFunction<ClipContext, BlockPos, T> context,
            Function<ClipContext, T> blockRayCaster
    ) {
        throw new AssertionError();
    }
}
