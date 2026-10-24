package com.trevorschoeny.oneclickoneblock.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The ticks a held button waits before it may break the next block. */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyDelay")
    void oneclickoneblock$setDestroyDelay(int ticks);
}
