package io.github.kawaiicakes.superbvs.mixin;

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.kawaiicakes.superbvs.SuperbVS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin extends Projectile {
    protected ProjectileEntityMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * This is basically a copy-paste of {@link org.valkyrienskies.mod.common.world.RaycastUtilsKt#clipIncludeShips(Level, ClipContext)}.
     * It's just reworked to fit in here, and also respects the blocks that should be ignored by SBW.
     */
    @WrapOperation(
            method = "rayTraceBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;performRayTrace(Lnet/minecraft/world/level/ClipContext;Ljava/util/function/BiFunction;Ljava/util/function/Function;)Ljava/lang/Object;"
            )
    )
    private static <T> T rayTraceIncludesShips(
            ClipContext clip, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> missFunction,
            Operation<T> original,
            @Local(argsOnly = true) LocalRef<Level> levelArg,
            @Local(argsOnly = true) LocalRef<Predicate<BlockState>> ignorePredicateArg
    ) {
        T originalClip = original.call(clip, hitFunction, missFunction);

        Level level;
        BlockHitResult clipNonGeneric;
        try {
            level = levelArg.get();
            clipNonGeneric = (BlockHitResult) originalClip;
        } catch (RuntimeException e) {
            SuperbVS.LOGGER.error(e);
            return originalClip;
        }

        //noinspection ConstantValue
        if (VSGameUtilsKt.getShipObjectWorld(level) == null) {
            SuperbVS.LOGGER.error(
                    "shipObjectWorld was empty for level raytrace, this should not be possible! " +
                            "Returning original result."
            );
            return originalClip;
        }

        BlockHitResult closestHit = clipNonGeneric;
        Vec3 closestHitPos = clipNonGeneric.getLocation();
        double closestHitDist = closestHitPos.distanceToSqr(clip.getFrom());

        AABBdc clipAABB = new AABBd(
                VectorConversionsMCKt.toJOML(clip.getFrom()),
                VectorConversionsMCKt.toJOML(clip.getTo())
        ).correctBounds();

        for (Ship ship : VSGameUtilsKt.getShipsIntersecting(level, clipAABB)) {
            Matrix4dc worldToShip = ship.getWorldToShip();
            Matrix4dc shipToWorld = ship.getShipToWorld();

            Vec3 shipStart = VectorConversionsMCKt.toMinecraft(
                    worldToShip.transformPosition(
                            VectorConversionsMCKt.toJOML(clip.getFrom())
                    )
            );
            Vec3 shipEnd = VectorConversionsMCKt.toMinecraft(
                    worldToShip.transformPosition(
                            VectorConversionsMCKt.toJOML(clip.getTo())
                    )
            );

            BlockHitResult shipHit = superbvs$vsClipWithPredicate(
                    level, clip, shipStart, shipEnd, ignorePredicateArg.get()
            );
            Vec3 shipHitPos = VectorConversionsMCKt.toMinecraft(
                    shipToWorld.transformPosition(
                            VectorConversionsMCKt.toJOML(shipHit.getLocation())
                    )
            );
            double shipHitDist = shipHitPos.distanceToSqr(clip.getFrom());

            if (shipHitDist < closestHitDist && shipHit.getType() != HitResult.Type.MISS) {
                closestHit = shipHit;
                closestHitPos = shipHitPos;
                closestHitDist = shipHitDist;
            }
        }

        closestHit.location = closestHitPos;

        return (T) closestHit;
    }

    @Unique
    private static BlockHitResult superbvs$vsClipWithPredicate(
            Level level,
            ClipContext context, Vec3 realStart, Vec3 realEnd,
            Predicate<BlockState> ignorePredicate
    ) {
        return RaycastUtilsKtInvoker.clip(
                realStart, realEnd, context,
                (raycastContext, blockPos) -> {
                    BlockState blockStateAt = level.getBlockState(blockPos);
                    if (ignorePredicate.test(blockStateAt)) return null;
                    FluidState fluidStateAt = level.getFluidState(blockPos);

                    VoxelShape blockShape = raycastContext.getBlockShape(blockStateAt, level, blockPos);
                    BlockHitResult blockResult = level.clipWithInteractionOverride(
                            realStart, realEnd, blockPos, blockShape, blockStateAt
                    );

                    VoxelShape fluidShape = raycastContext.getFluidShape(fluidStateAt, level, blockPos);
                    BlockHitResult fluidResult = fluidShape.clip(realStart, realEnd, blockPos);

                    double distanceToBlock = (blockResult == null)
                            ? Double.MAX_VALUE
                            : realStart.distanceToSqr(blockResult.getLocation());
                    double distanceToFluid = (fluidResult == null)
                            ? Double.MAX_VALUE
                            : realEnd.distanceToSqr(fluidResult.getLocation());

                    return (distanceToBlock <= distanceToFluid) ? blockResult : fluidResult;
                },
                (raycastContext) -> {
                    Vec3 delta = realStart.subtract(realEnd);
                    return BlockHitResult.miss(
                            realEnd, Direction.getNearest(delta.x, delta.y, delta.z), BlockPos.containing(realEnd)
                    );
                }
        );
    }
}
