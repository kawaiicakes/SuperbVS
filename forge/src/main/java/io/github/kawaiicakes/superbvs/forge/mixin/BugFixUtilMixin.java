package io.github.kawaiicakes.superbvs.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.mod.util.BugFixUtil;

@Mixin(BugFixUtil.class)
public abstract class BugFixUtilMixin {
    @WrapMethod(method = "isCollisionBoxToBig")
    public boolean lol(AABB aabb, Operation<Boolean> original) {
        boolean toReturn = original.call(aabb);
        if (toReturn) {
            LogManager.getLogger().warn(ExceptionUtils.getStackTrace(new RuntimeException()));
        }
        return toReturn;
    }
}
