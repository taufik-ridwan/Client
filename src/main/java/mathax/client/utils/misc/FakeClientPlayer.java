package mathax.client.utils.misc;

import mathax.client.mixin.DimensionTypeAccessor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.text.LiteralText;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import static mathax.client.MatHax.mc;

public class FakeClientPlayer {
    private static ClientWorld world;
    private static PlayerEntity player;
    private static PlayerListEntry playerListEntry;

    private static String lastId;
    private static boolean needsNewEntry;

    public static void init() {
        // TODO: Maybe we can pass null for telemetry sender because this world instance is never actually gonna send anything
        world = new ClientWorld(new ClientPlayNetworkHandler(mc, null, new ClientConnection(NetworkSide.CLIENTBOUND), mc.getSession().getProfile(), mc.createTelemetrySender()), new ClientWorld.Properties(Difficulty.NORMAL, false, false), World.OVERWORLD, DimensionTypeAccessor.getOverworld(), 1, 1, mc::getProfiler, new WorldRenderer(mc, new BufferBuilderStorage()), false, 0);
    }

    public static PlayerEntity getPlayer() {
        String id = mc.getSession().getUuid();

        if (player == null || (!id.equals(lastId))) {
            player = new OtherClientPlayerEntity(world, mc.getSession().getProfile());

            lastId = id;
            needsNewEntry = true;
        }

        return player;
    }

    public static PlayerListEntry getPlayerListEntry() {
        if (playerListEntry == null || needsNewEntry) {
            playerListEntry = new PlayerListEntry(PlayerListEntryFactory.create(mc.getSession().getProfile(), 0, GameMode.SURVIVAL, new LiteralText(mc.getSession().getProfile().getName())));
            needsNewEntry = false;
        }

        return playerListEntry;
    }
}
