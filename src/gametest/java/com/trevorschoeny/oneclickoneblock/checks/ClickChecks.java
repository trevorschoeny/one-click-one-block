package com.trevorschoeny.oneclickoneblock.checks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import testgallery.SceneWorld;

/**
 * Checks for the clicks scene (test-gallery/scenes/one-click-one-block/clicks.json).
 *
 * <p>Each checked station is a dirt or stone block 3 wide, 3 high and 5 deep. The player
 * stands against its front, looking straight along the middle row, so every block
 * broken in that row uncovers the next one under the crosshair: exactly the case where
 * vanilla breaks two or three blocks with one click.
 */
public class ClickChecks implements FabricClientGameTest {
    /**
     * A quick click is 1 to 3 ticks. 4 is a slow one, still short of the 5-tick pause:
     * vanilla breaks 4 blocks with it.
     */
    private static final int CLICK_TICKS = 4;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "one-click-one-block", "clicks")) {
            // Haste II: with it the Efficiency V pickaxe breaks stone instantly.
            scene.world().getServer().runCommand("effect give @p minecraft:haste infinite 1 true");

            // Station "oneclickoneblock:shovel_click": one click, one block; a second click, one more.
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[0]);
            JsonObject dirt = scene.station("oneclickoneblock:shovel_click");
            faceRow(ctx, scene, dirt);
            click(ctx);
            expectBroken(ctx, scene, dirt, 1, "one click with the shovel");
            click(ctx);
            expectBroken(ctx, scene, dirt, 2, "a second click with the shovel");

            // Station "oneclickoneblock:pickaxe_click": the same with Haste on stone.
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[1]);
            JsonObject stone = scene.station("oneclickoneblock:pickaxe_click");
            faceRow(ctx, scene, stone);
            click(ctx);
            expectBroken(ctx, scene, stone, 1, "one click with the pickaxe");

            // Station "oneclickoneblock:hold": holding still digs in a row. 4 of the 5 blocks
            // are in reach; vanilla takes them on ticks 1, 7, 8 and 9 of the hold.
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[0]);
            JsonObject hold = scene.station("oneclickoneblock:hold");
            faceRow(ctx, scene, hold);
            ctx.getInput().holdKeyFor(options -> options.keyAttack, 20);
            ctx.waitTicks(5);
            int held = broken(ctx, scene, hold);
            if (held < 4) throw new AssertionError("holding for a second should break the 4 blocks in reach; broke " + held);
        }
    }

    /** Stands against the front of the station's block, eyes level with its middle row, facing south. */
    private static void faceRow(ClientGameTestContext ctx, SceneWorld.Opened scene, JsonObject station) {
        BlockPos front = row(station, 0);
        scene.world().getServer().runCommand("tp @p " + (front.getX() + 0.5) + " " + (front.getY() - 1) + " " + (front.getZ() - 0.5) + " 0 0");
        ctx.waitTicks(5);
    }

    private static void click(ClientGameTestContext ctx) {
        ctx.getInput().holdKeyFor(options -> options.keyAttack, CLICK_TICKS);
        ctx.waitTicks(10);   // past the pause, and the server has answered
    }

    private static void expectBroken(ClientGameTestContext ctx, SceneWorld.Opened scene, JsonObject station, int want, String what) {
        int got = broken(ctx, scene, station);
        if (got != want) throw new AssertionError(what + " should leave " + want + " block(s) broken in the row; broke " + got);
    }

    /** Blocks gone from the middle row, read on the server, which has the final word. */
    private static int broken(ClientGameTestContext ctx, SceneWorld.Opened scene, JsonObject station) {
        return scene.world().getServer().computeOnServer(server -> {
            int n = 0;
            for (int i = 0; i < 5; i++)
                if (server.overworld().getBlockState(row(station, i)).isAir()) n++;
            return n;
        });
    }

    /** The i-th block of the middle row, front to back: the scene fills x 1..3, y 0..2, z 1..5. */
    private static BlockPos row(JsonObject station, int i) {
        JsonArray o = station.getAsJsonArray("origin");
        return new BlockPos(o.get(0).getAsInt() + 2, o.get(1).getAsInt() + 1, o.get(2).getAsInt() + 1 + i);
    }
}
