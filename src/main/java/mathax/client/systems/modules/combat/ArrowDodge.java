package mathax.client.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import mathax.client.eventbus.EventHandler;
import mathax.client.events.world.TickEvent;
import mathax.client.mixin.ProjectileInGroundAccessor;
import mathax.client.settings.*;
import mathax.client.systems.modules.Categories;
import mathax.client.systems.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrowDodge extends Module {
    private final List<Vec3d> possibleMoveDirections = Arrays.asList(
        new Vec3d(1, 0, 1),
        new Vec3d(0, 0, 1),
        new Vec3d(-1, 0, 1),
        new Vec3d(1, 0, 0),
        new Vec3d(-1, 0, 0),
        new Vec3d(1, 0, -1),
        new Vec3d(0, 0, -1),
        new Vec3d(-1, 0, -1)
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");

    // General

    private final Setting<Integer> arrowLookahead = sgGeneral.add(new IntSetting.Builder()
        .name("arrow-lookahead")
        .description("How many steps into the future should be taken into consideration when deciding the direction")
        .defaultValue(500)
        .range(1, 750)
        .sliderRange(1, 750)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("ignore-projectiles")
        .description("Won't dodge these projectiles.")
        .build()
    );

    // Movement

    private final Setting<MoveType> moveType = sgMovement.add(new EnumSetting.Builder<MoveType>()
        .name("move-type")
        .description("The way you are moved by this module")
        .defaultValue(MoveType.Client)
        .build()
    );

    private final Setting<Double> moveSpeed = sgMovement.add(new DoubleSetting.Builder()
        .name("move-speed")
        .description("How fast should you be when dodging arrow")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 5)
        .build()
    );

    private final Setting<Boolean> groundCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("ground-check")
        .description("Tries to prevent you from falling to your death.")
        .defaultValue(true)
        .build()
    );

    public ArrowDodge() {
        super(Categories.Combat, Items.ARROW, "arrow-dodge", "Tries to dodge arrows coming at you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Box playerHitbox = mc.player.getBoundingBox();
        playerHitbox = playerHitbox.expand(0.6);

        double speed = moveSpeed.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity)) continue;
            if (entity instanceof PersistentProjectileEntity && ((ProjectileInGroundAccessor) entity).getInGround()) continue;
            if (entities.get().getBoolean(entity.getType())) continue;

            List<Box> futureArrowHitboxes = new ArrayList<>();

            for (int i = 0; i < arrowLookahead.get(); i++) {
                Vec3d nextPos = entity.getPos().add(entity.getVelocity().multiply(i / 5.0f));
                futureArrowHitboxes.add(new Box(nextPos.subtract(entity.getBoundingBox().getXLength() / 2, 0, entity.getBoundingBox().getZLength() / 2), nextPos.add(entity.getBoundingBox().getXLength() / 2, entity.getBoundingBox().getYLength(), entity.getBoundingBox().getZLength() / 2)));
            }

            for (Box arrowHitbox : futureArrowHitboxes) {
                if (!playerHitbox.intersects(arrowHitbox)) continue;

                Collections.shuffle(possibleMoveDirections);

                boolean didMove = false;

                for (Vec3d direction : possibleMoveDirections) {
                    Vec3d velocity = direction.multiply(speed);
                    if (isValid(velocity, futureArrowHitboxes, playerHitbox)) {
                        move(velocity);
                        didMove = true;
                        break;
                    }
                }

                if (!didMove) {
                    double yaw = Math.toRadians(entity.getYaw());
                    double pitch = Math.toRadians(entity.getPitch());
                    move(Math.sin(yaw) * Math.cos(pitch) * speed, Math.sin(pitch) * speed, -Math.cos(yaw) * Math.cos(pitch) * speed);
                }
            }
        }
    }

    private void move(Vec3d vel) {
        move(vel.x, vel.y, vel.z);
    }

    private void move(double velX, double velY, double velZ) {
        switch (moveType.get()) {
            case Client -> mc.player.setVelocity(velX, velY, velZ);
            case Packet -> {
                Vec3d newPos = mc.player.getPos().add(velX, velY, velZ);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y - 0.01, newPos.z, true));
            }
        }
    }

    private boolean isValid(Vec3d velocity, List<Box> futureArrowHitboxes, Box playerHitbox) {
        BlockPos blockPos = null;
        for (Box futureArrowHitbox : futureArrowHitboxes) {
            Box newPlayerPos = playerHitbox.offset(velocity);
            if (futureArrowHitbox.intersects(newPlayerPos)) return false;

            blockPos = mc.player.getBlockPos().add(velocity.x, velocity.y, velocity.z);
            if (mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos) != VoxelShapes.empty()) return false;
        }

        if (groundCheck.get() && blockPos != null) return mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()) != VoxelShapes.empty();

        return true;
    }

    public enum MoveType {
        Client("Client"),
        Packet("Packet");

        private final String title;

        MoveType(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
