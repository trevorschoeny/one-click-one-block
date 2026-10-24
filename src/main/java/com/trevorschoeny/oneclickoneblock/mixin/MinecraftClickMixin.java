package com.trevorschoeny.oneclickoneblock.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * One click, one block.
 *
 * <p>Vanilla mining runs on two paths. A fresh click goes through {@code startAttack},
 * which calls {@code startDestroyBlock}. Every later tick the button stays down goes
 * through {@code continueDestroyBlock}, which does nothing while {@code destroyDelay}
 * counts down and then mines whatever the crosshair is on.
 *
 * <p>A normal break, and every creative break, sets {@code destroyDelay = 5}: the short
 * pause before a held button starts on the next block. An instant survival break
 * (Efficiency on dirt, Haste on stone) sets nothing. A click lasts a few ticks, so the
 * block behind the first one breaks on the next tick of the same click, and the one
 * behind that on the tick after.
 *
 * <p>This gives a click's instant break a shorter pause of its own, {@link #CLICK_PAUSE_TICKS}
 * (Trev, 2026-10-24: 3 feels better than vanilla's 5). Let go sooner (a click)
 * and only one block broke; the next click starts fresh, since {@code startDestroyBlock}
 * never waits on the delay. Keep holding and the pause runs out, after which holding
 * mines exactly as vanilla does: breaks during a hold go through
 * {@code continueDestroyBlock} and are left alone, so the speed of a hold doesn't change.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClickMixin {

    /** Ticks a held button waits after a click's instant break. A click is 1 to 3 ticks. */
    private static final int CLICK_PAUSE_TICKS = 3;

    @Shadow public ClientLevel level;

    @WrapOperation(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean oneclickoneblock$pauseAfterInstantBreak(MultiPlayerGameMode gameMode, BlockPos pos, Direction direction,
                                                           Operation<Boolean> original) {
        boolean result = original.call(gameMode, pos, direction);
        // The block is already air client-side when it broke instantly: vanilla predicts
        // the break before the server answers, and startAttack reads the same signal
        // right after this call. Creative lands here too, so it's left with the 5 vanilla set.
        if (level.getBlockState(pos).isAir() && !gameMode.getPlayerMode().isCreative()) {
            ((MultiPlayerGameModeAccessor) gameMode).oneclickoneblock$setDestroyDelay(CLICK_PAUSE_TICKS);
        }
        return result;
    }
}
