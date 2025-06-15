package io.github.kawaiicakes.superbvs.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(BlockGetter.class)
public abstract class BlockGetterMixin {
    @WrapMethod(method = "clip")
    public BlockHitResult lol(ClipContext context, Operation<BlockHitResult> original) {
        try {
            Level level = Level.class.cast(this);

            if (VSGameUtilsKt.getShipManagingPos(level, context.getTo()) !=
                    VSGameUtilsKt.getShipManagingPos(level, context.getFrom())) {
                LogManager.getLogger().warn(ExceptionUtils.getStackTrace(new RuntimeException()));
            }
        } catch (Exception ignored) {
            return original.call(context);
        }

        return original.call(context);
    }
}
