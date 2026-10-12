package com.example.lokistructures;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.util.hit.HitResult;
import org.joml.Vector3f;

/** Loki's Scepter: right-click arcane blast; sneak-right-click illusion cloak. */
public final class LokiScepterItem extends Item {
    private static final DustParticleEffect GREEN_MAGIC =
            new DustParticleEffect(new Vector3f(0.1f, 0.95f, 0.35f), 1.3f);

    public LokiScepterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.success(stack, true);
        }

        if (user.isSneaking()) {
            // Escape trick: invisibility + mobility, no permanent invulnerability.
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 12, 0));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 12, 1));
            serverWorld.spawnParticles(GREEN_MAGIC, user.getX(), user.getBodyY(.8), user.getZ(),
                    55, .5, .8, .5, .04);
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, user.getX(), user.getBodyY(.6), user.getZ(),
                    25, .4, .8, .4, .05);
            serverWorld.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL,
                    SoundCategory.PLAYERS, 1.0f, 1.1f);
            user.getItemCooldownManager().set(this, 20 * 18);
        } else {
            castBeam(serverWorld, user);
            user.getItemCooldownManager().set(this, 20 * 5);
        }
        return TypedActionResult.success(stack, false);
    }

    private void castBeam(ServerWorld world, PlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0F).normalize();
        Vec3d end = start.add(direction.multiply(24));
        var block = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        double distance = block.getType() == HitResult.Type.MISS ? 24 : start.distanceTo(block.getPos());
        end = start.add(direction.multiply(distance));

        // Thin green beam, small packet count; damage uses server-side validation.
        for (double d = .5; d <= distance; d += .65) {
            Vec3d p = start.add(direction.multiply(d));
            world.spawnParticles(GREEN_MAGIC, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }

        LivingEntity victim = null;
        double closest = distance + .001;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class,
                new Box(start, end).expand(1.5), e -> e.isAlive() && e != player)) {
            if (entity instanceof PlayerEntity target && (target.isSpectator() || target.isCreative())) continue;
            Vec3d delta = entity.getBoundingBox().getCenter().subtract(start);
            double along = delta.dotProduct(direction);
            if (along < 0 || along > distance || along >= closest) continue;
            double sideways = delta.subtract(direction.multiply(along)).lengthSquared();
            double width = .55 + entity.getWidth() / 2;
            if (sideways > width * width) continue;
            victim = entity;
            closest = along;
        }
        if (victim != null) {
            victim.damage(player.getDamageSources().playerAttack(player), 12.0F);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 5, 2));
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 5, 0));
            Vec3d p = victim.getBoundingBox().getCenter();
            world.spawnParticles(GREEN_MAGIC, p.x, p.y, p.z, 32, .35, .6, .35, .02);
            world.spawnParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 18, .4, .5, .4, .04);
        } else if (block.getType() != HitResult.Type.MISS) {
            Vec3d p = block.getPos();
            world.spawnParticles(GREEN_MAGIC, p.x, p.y, p.z, 22, .3, .3, .3, .02);
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS, .9f, .8f);
    }
}
