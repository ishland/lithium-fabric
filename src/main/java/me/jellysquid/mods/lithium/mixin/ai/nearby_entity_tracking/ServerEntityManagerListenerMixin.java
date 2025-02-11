package me.jellysquid.mods.lithium.mixin.ai.nearby_entity_tracking;

import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityListenerMulti;
import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.EntityTrackingStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net/minecraft/server/world/ServerEntityManager$Listener")
public class ServerEntityManagerListenerMixin<T extends EntityLike> {
    @Shadow
    @Final
    private T entity;

    @Final
    @SuppressWarnings("ShadowTarget")
    @Shadow
    ServerEntityManager<T> manager;

    @Shadow
    private long sectionPos;

    @Inject(
            method = "updateEntityPosition()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityTrackingSection;add(Lnet/minecraft/world/entity/EntityLike;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onAddEntity(CallbackInfo ci, BlockPos blockPos, long newPos, EntityTrackingStatus entityTrackingStatus, EntityTrackingSection<T> entityTrackingSection) {
        NearbyEntityListenerMulti listener = ((NearbyEntityListenerProvider) this.entity).getListener();
        if (listener != null)
        {
            //noinspection unchecked
            listener.forEachChunkInRangeChange(
                    ((ServerEntityManagerAccessor<T>) this.manager).getCache(),
                    ChunkSectionPos.from(this.sectionPos),
                    ChunkSectionPos.from(newPos)
            );
        }
    }

    @Inject(
            method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onRemoveEntity(Entity.RemovalReason reason, CallbackInfo ci) {
        NearbyEntityListenerMulti listener = ((NearbyEntityListenerProvider) this.entity).getListener();
        if (listener != null) {
            //noinspection unchecked
            listener.forEachChunkInRangeChange(
                    ((ServerEntityManagerAccessor<T>) this.manager).getCache(),
                    ChunkSectionPos.from(this.sectionPos),
                    null
            );
        }
    }
}
