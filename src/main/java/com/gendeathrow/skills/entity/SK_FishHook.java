package com.gendeathrow.skills.entity;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.WeightedRandomFishable;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.gendeathrow.skills.common.SkillTrackerData;
import com.gendeathrow.skills.common.Skill_TrackerManager;
import com.gendeathrow.skills.items.SK_FishingRod;
import com.gendeathrow.skills.skill_tree.resource_gathering.FishingSkill;


public class SK_FishHook extends Entity implements IEntityAdditionalSpawnData
{
    public static final List JUNK = Arrays.asList(new WeightedRandomFishable[] {(new WeightedRandomFishable(new ItemStack(Items.leather_boots), 10)).setMaxDamagePercent(0.9F), new WeightedRandomFishable(new ItemStack(Items.leather), 10), new WeightedRandomFishable(new ItemStack(Items.bone), 10), new WeightedRandomFishable(new ItemStack(Items.potionitem), 10), new WeightedRandomFishable(new ItemStack(Items.string), 5), (new WeightedRandomFishable(new ItemStack(Items.fishing_rod), 2)).setMaxDamagePercent(0.9F), new WeightedRandomFishable(new ItemStack(Items.bowl), 10), new WeightedRandomFishable(new ItemStack(Items.stick), 5), new WeightedRandomFishable(new ItemStack(Items.dye, 10, EnumDyeColor.BLACK.getDyeDamage()), 1), new WeightedRandomFishable(new ItemStack(Blocks.tripwire_hook), 10), new WeightedRandomFishable(new ItemStack(Items.rotten_flesh), 10)});
    public static final List TREASURE = Arrays.asList(new WeightedRandomFishable[] {new WeightedRandomFishable(new ItemStack(Blocks.waterlily), 1), new WeightedRandomFishable(new ItemStack(Items.name_tag), 1), new WeightedRandomFishable(new ItemStack(Items.saddle), 1), (new WeightedRandomFishable(new ItemStack(Items.bow), 1)).setMaxDamagePercent(0.25F).setEnchantable(), (new WeightedRandomFishable(new ItemStack(Items.fishing_rod), 1)).setMaxDamagePercent(0.25F).setEnchantable(), (new WeightedRandomFishable(new ItemStack(Items.book), 1)).setEnchantable()});
    public static final List FISH = Arrays.asList(new WeightedRandomFishable[] {new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.COD.getMetadata()), 60), new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.SALMON.getMetadata()), 25), new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.CLOWNFISH.getMetadata()), 2), new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.PUFFERFISH.getMetadata()), 13)});
    private int xTile;
    private int yTile;
    private int zTile;
    private Block inTile;
    private boolean inGround;
    public int shake;
    public EntityPlayer angler;
    public SkillTrackerData tracker;
    private FishingSkill fishingSkill;
    private int ticksInGround;
    private int ticksInAir;
    private int ticksCatchable;
    private int ticksCaughtDelay;
    private int ticksCatchableDelay;
    private float fishApproachAngle;
    public Entity caughtEntity;
    private int fishPosRotationIncrements;
    private double fishX;
    private double fishY;
    private double fishZ;
    private double fishYaw;
    private double fishPitch;
    @SideOnly(Side.CLIENT)
    private double clientMotionX;
    @SideOnly(Side.CLIENT)
    private double clientMotionY;
    @SideOnly(Side.CLIENT)
    private double clientMotionZ;
    private static final String __OBFID = "CL_00001663";

    public static List func_174855_j()
    {
        return FISH;
    }

    public SK_FishHook(World worldIn)
    {
        super(worldIn);
        this.xTile = -1;
        this.yTile = -1;
        this.zTile = -1;
        this.setSize(0.25F, 0.25F);
        this.ignoreFrustumCheck = true;
    }

    @SideOnly(Side.CLIENT)
    public SK_FishHook(World worldIn, double p_i1765_2_, double p_i1765_4_, double p_i1765_6_, EntityPlayer p_i1765_8_)
    {
        this(worldIn);
        this.setPosition(p_i1765_2_, p_i1765_4_, p_i1765_6_);
        this.ignoreFrustumCheck = true;
        this.angler = p_i1765_8_;
        this.tracker = Skill_TrackerManager.lookupTracker(p_i1765_8_);
        this.tracker.fishingEntity = this;
    }

    public SK_FishHook(World worldIn, EntityPlayer fishingPlayer)
    {
        super(worldIn);
        this.xTile = -1;
        this.yTile = -1;
        this.zTile = -1;
        this.ignoreFrustumCheck = true;
        this.angler = fishingPlayer;
        
        if(worldIn.isRemote) System.out.println(this.angler.getName() + "<client");
        this.tracker = Skill_TrackerManager.lookupTracker(fishingPlayer);
        this.tracker.fishingEntity = this;
        this.setSize(0.25F, 0.25F);
        this.setLocationAndAngles(fishingPlayer.posX, fishingPlayer.posY + (double)fishingPlayer.getEyeHeight(), fishingPlayer.posZ, fishingPlayer.rotationYaw, fishingPlayer.rotationPitch);
        this.posX -= (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
        this.posY -= 0.10000000149011612D;
        this.posZ -= (double)(MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
        this.setPosition(this.posX, this.posY, this.posZ);
        float f = 0.4F;
        this.motionX = (double)(-MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * f);
        this.motionZ = (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * f);
        this.motionY = (double)(-MathHelper.sin(this.rotationPitch / 180.0F * (float)Math.PI) * f);
        this.handleHookCasting(this.motionX, this.motionY, this.motionZ, 1.5F, 1.0F);

    }

    protected void entityInit() {}

    /**
     * Checks if the entity is in range to render by using the past in distance and comparing it to its average edge
     * length * 64 * renderDistanceWeight Args: distance
     */
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance)
    {
        double d1 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0D;
        d1 *= 64.0D;
        return distance < d1 * d1;
    }

    public void handleHookCasting(double p_146035_1_, double p_146035_3_, double p_146035_5_, float p_146035_7_, float p_146035_8_)
    {
        float f2 = MathHelper.sqrt_double(p_146035_1_ * p_146035_1_ + p_146035_3_ * p_146035_3_ + p_146035_5_ * p_146035_5_);
        p_146035_1_ /= (double)f2;
        p_146035_3_ /= (double)f2;
        p_146035_5_ /= (double)f2;
        p_146035_1_ += this.rand.nextGaussian() * 0.007499999832361937D * (double)p_146035_8_;
        p_146035_3_ += this.rand.nextGaussian() * 0.007499999832361937D * (double)p_146035_8_;
        p_146035_5_ += this.rand.nextGaussian() * 0.007499999832361937D * (double)p_146035_8_;
        p_146035_1_ *= (double)p_146035_7_;
        p_146035_3_ *= (double)p_146035_7_;
        p_146035_5_ *= (double)p_146035_7_;
        this.motionX = p_146035_1_;
        this.motionY = p_146035_3_;
        this.motionZ = p_146035_5_;
        float f3 = MathHelper.sqrt_double(p_146035_1_ * p_146035_1_ + p_146035_5_ * p_146035_5_);
        this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(p_146035_1_, p_146035_5_) * 180.0D / Math.PI);
        this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(p_146035_3_, (double)f3) * 180.0D / Math.PI);
        this.ticksInGround = 0;
    }

    @SideOnly(Side.CLIENT)
    public void func_180426_a(double p_180426_1_, double p_180426_3_, double p_180426_5_, float p_180426_7_, float p_180426_8_, int p_180426_9_, boolean p_180426_10_)
    {
        this.fishX = p_180426_1_;
        this.fishY = p_180426_3_;
        this.fishZ = p_180426_5_;
        this.fishYaw = (double)p_180426_7_;
        this.fishPitch = (double)p_180426_8_;
        this.fishPosRotationIncrements = p_180426_9_;
        this.motionX = this.clientMotionX;
        this.motionY = this.clientMotionY;
        this.motionZ = this.clientMotionZ;
    }

    /**
     * Sets the velocity to the args. Args: x, y, z
     */
    @SideOnly(Side.CLIENT)
    public void setVelocity(double x, double y, double z)
    {
        this.clientMotionX = this.motionX = x;
        this.clientMotionY = this.motionY = y;
        this.clientMotionZ = this.motionZ = z;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        super.onUpdate();

        if (this.fishPosRotationIncrements > 0)
        {
            double d7 = this.posX + (this.fishX - this.posX) / (double)this.fishPosRotationIncrements;
            double d8 = this.posY + (this.fishY - this.posY) / (double)this.fishPosRotationIncrements;
            double d9 = this.posZ + (this.fishZ - this.posZ) / (double)this.fishPosRotationIncrements;
            double d1 = MathHelper.wrapAngleTo180_double(this.fishYaw - (double)this.rotationYaw);
            this.rotationYaw = (float)((double)this.rotationYaw + d1 / (double)this.fishPosRotationIncrements);
            this.rotationPitch = (float)((double)this.rotationPitch + (this.fishPitch - (double)this.rotationPitch) / (double)this.fishPosRotationIncrements);
            --this.fishPosRotationIncrements;
            this.setPosition(d7, d8, d9);
            this.setRotation(this.rotationYaw, this.rotationPitch);
        }
        else
        {
            if (!this.worldObj.isRemote)
            {
                ItemStack itemstack = this.angler.getCurrentEquippedItem();

                if (this.angler.isDead || !this.angler.isEntityAlive() || itemstack == null || itemstack.getItem() != SK_FishingRod.fishingrod || this.getDistanceSqToEntity(this.angler) > 1024.0D)
                {
                	System.out.println("something is wrong");
                    this.setDead();
                    this.tracker.fishingEntity = null;
                    return;
                }

                if (this.caughtEntity != null)
                {
                    if (!this.caughtEntity.isDead)
                    {
                        this.posX = this.caughtEntity.posX;
                        double d12 = (double)this.caughtEntity.height;
                        this.posY = this.caughtEntity.getEntityBoundingBox().minY + d12 * 0.8D;
                        this.posZ = this.caughtEntity.posZ;
                        return;
                    }

                    this.caughtEntity = null;
                }
            }

            if (this.shake > 0)
            {
                --this.shake;
            }

            if (this.inGround)
            {
                if (this.worldObj.getBlockState(new BlockPos(this.xTile, this.yTile, this.zTile)).getBlock() == this.inTile)
                {
                    ++this.ticksInGround;

                    if (this.ticksInGround == 1200)
                    {
                    	System.out.println("inground?");
                        this.setDead();
                    }

                    return;
                }

                this.inGround = false;
                this.motionX *= (double)(this.rand.nextFloat() * 0.2F);
                this.motionY *= (double)(this.rand.nextFloat() * 0.2F);
                this.motionZ *= (double)(this.rand.nextFloat() * 0.2F);
                this.ticksInGround = 0;
                this.ticksInAir = 0;
            }
            else
            {
                ++this.ticksInAir;
            }

            Vec3 vec31 = new Vec3(this.posX, this.posY, this.posZ);
            Vec3 vec3 = new Vec3(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec31, vec3);
            vec31 = new Vec3(this.posX, this.posY, this.posZ);
            vec3 = new Vec3(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

            if (movingobjectposition != null)
            {
                vec3 = new Vec3(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
            }

            Entity entity = null;
            List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;
            double d2;

            for (int i = 0; i < list.size(); ++i)
            {
                Entity entity1 = (Entity)list.get(i);

                if (entity1.canBeCollidedWith() && (entity1 != this.angler || this.ticksInAir >= 5))
                {
                    float f = 0.3F;
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand((double)f, (double)f, (double)f);
                    MovingObjectPosition movingobjectposition1 = axisalignedbb.calculateIntercept(vec31, vec3);

                    if (movingobjectposition1 != null)
                    {
                        d2 = vec31.distanceTo(movingobjectposition1.hitVec);

                        if (d2 < d0 || d0 == 0.0D)
                        {
                            entity = entity1;
                            d0 = d2;
                        }
                    }
                }
            }

            if (entity != null)
            {
                movingobjectposition = new MovingObjectPosition(entity);
            }

            if (movingobjectposition != null)
            {
                if (movingobjectposition.entityHit != null)
                {
                    if (movingobjectposition.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.angler), 0.0F))
                    {
                        this.caughtEntity = movingobjectposition.entityHit;
                    }
                }
                else
                {
                    this.inGround = true;
                }
            }

            if (!this.inGround)
            {
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                float f5 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
                this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

                for (this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f5) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
                {
                    ;
                }

                while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
                {
                    this.prevRotationPitch += 360.0F;
                }

                while (this.rotationYaw - this.prevRotationYaw < -180.0F)
                {
                    this.prevRotationYaw -= 360.0F;
                }

                while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
                {
                    this.prevRotationYaw += 360.0F;
                }

                this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
                this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
                float f6 = 0.92F;

                if (this.onGround || this.isCollidedHorizontally)
                {
                    f6 = 0.5F;
                }

                byte b0 = 5;
                double d10 = 0.0D;
                double d5;

                for (int j = 0; j < b0; ++j)
                {
                    AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
                    double d3 = axisalignedbb1.maxY - axisalignedbb1.minY;
                    double d4 = axisalignedbb1.minY + d3 * (double)j / (double)b0;
                    d5 = axisalignedbb1.minY + d3 * (double)(j + 1) / (double)b0;
                    AxisAlignedBB axisalignedbb2 = new AxisAlignedBB(axisalignedbb1.minX, d4, axisalignedbb1.minZ, axisalignedbb1.maxX, d5, axisalignedbb1.maxZ);

                    if (this.worldObj.isAABBInMaterial(axisalignedbb2, Material.water))
                    {
                        d10 += 1.0D / (double)b0;
                    }
                }

                if (!this.worldObj.isRemote && d10 > 0.0D)
                {
                    WorldServer worldserver = (WorldServer)this.worldObj;
                    int k = 1;
                    BlockPos blockpos = (new BlockPos(this)).up();

                    if (this.rand.nextFloat() < 0.25F && this.worldObj.canLightningStrike(blockpos))
                    {
                        k = 2;
                    }

                    if (this.rand.nextFloat() < 0.5F && !this.worldObj.canSeeSky(blockpos))
                    {
                        --k;
                    }

                    if (this.ticksCatchable > 0)
                    {
                        --this.ticksCatchable;

                        if (this.ticksCatchable <= 0)
                        {
                            this.ticksCaughtDelay = 0;
                            this.ticksCatchableDelay = 0;
                        }
                    }
                    else
                    {
                        float f1;
                        float f2;
                        double d6;
                        float f7;
                        double d11;

                        if (this.ticksCatchableDelay > 0)
                        {
                            this.ticksCatchableDelay -= k;

                            if (this.ticksCatchableDelay <= 0)
                            {
                                this.motionY -= 0.20000000298023224D;
                                this.playSound("random.splash", 0.25F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                                f1 = (float)MathHelper.floor_double(this.getEntityBoundingBox().minY);
                                worldserver.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX, (double)(f1 + 1.0F), this.posZ, (int)(1.0F + this.width * 20.0F), (double)this.width, 0.0D, (double)this.width, 0.20000000298023224D, new int[0]);
                                worldserver.spawnParticle(EnumParticleTypes.WATER_WAKE, this.posX, (double)(f1 + 1.0F), this.posZ, (int)(1.0F + this.width * 20.0F), (double)this.width, 0.0D, (double)this.width, 0.20000000298023224D, new int[0]);
                                this.ticksCatchable = MathHelper.getRandomIntegerInRange(this.rand, 10, 30);
                            }
                            else
                            {
                                this.fishApproachAngle = (float)((double)this.fishApproachAngle + this.rand.nextGaussian() * 4.0D);
                                f1 = this.fishApproachAngle * 0.017453292F;
                                f7 = MathHelper.sin(f1);
                                f2 = MathHelper.cos(f1);
                                d5 = this.posX + (double)(f7 * (float)this.ticksCatchableDelay * 0.1F);
                                d11 = (double)((float)MathHelper.floor_double(this.getEntityBoundingBox().minY) + 1.0F);
                                d6 = this.posZ + (double)(f2 * (float)this.ticksCatchableDelay * 0.1F);

                                if (this.rand.nextFloat() < 0.15F)
                                {
                                    worldserver.spawnParticle(EnumParticleTypes.WATER_BUBBLE, d5, d11 - 0.10000000149011612D, d6, 1, (double)f7, 0.1D, (double)f2, 0.0D, new int[0]);
                                }

                                float f3 = f7 * 0.04F;
                                float f4 = f2 * 0.04F;
                                worldserver.spawnParticle(EnumParticleTypes.WATER_WAKE, d5, d11, d6, 0, (double)f4, 0.01D, (double)(-f3), 1.0D, new int[0]);
                                worldserver.spawnParticle(EnumParticleTypes.WATER_WAKE, d5, d11, d6, 0, (double)(-f4), 0.01D, (double)f3, 1.0D, new int[0]);
                            }
                        }
                        else if (this.ticksCaughtDelay > 0)
                        {
                            this.ticksCaughtDelay -= k;
                            f1 = 0.15F;

                            if (this.ticksCaughtDelay < 20)
                            {
                                f1 = (float)((double)f1 + (double)(20 - this.ticksCaughtDelay) * 0.05D);
                            }
                            else if (this.ticksCaughtDelay < 40)
                            {
                                f1 = (float)((double)f1 + (double)(40 - this.ticksCaughtDelay) * 0.02D);
                            }
                            else if (this.ticksCaughtDelay < 60)
                            {
                                f1 = (float)((double)f1 + (double)(60 - this.ticksCaughtDelay) * 0.01D);
                            }

                            if (this.rand.nextFloat() < f1)
                            {
                                f7 = MathHelper.randomFloatClamp(this.rand, 0.0F, 360.0F) * 0.017453292F;
                                f2 = MathHelper.randomFloatClamp(this.rand, 25.0F, 60.0F);
                                d5 = this.posX + (double)(MathHelper.sin(f7) * f2 * 0.1F);
                                d11 = (double)((float)MathHelper.floor_double(this.getEntityBoundingBox().minY) + 1.0F);
                                d6 = this.posZ + (double)(MathHelper.cos(f7) * f2 * 0.1F);
                                worldserver.spawnParticle(EnumParticleTypes.WATER_SPLASH, d5, d11, d6, 2 + this.rand.nextInt(2), 0.10000000149011612D, 0.0D, 0.10000000149011612D, 0.0D, new int[0]);
                            }

                            if (this.ticksCaughtDelay <= 0)
                            {
                                this.fishApproachAngle = MathHelper.randomFloatClamp(this.rand, 0.0F, 360.0F);
                                this.ticksCatchableDelay = MathHelper.getRandomIntegerInRange(this.rand, 20, 80);
                            }
                        }
                        else
                        {
                            this.ticksCaughtDelay = MathHelper.getRandomIntegerInRange(this.rand, 100, 900);
                            this.ticksCaughtDelay -= EnchantmentHelper.getLureModifier(this.angler) * 20 * 5;
                        }
                    }

                    if (this.ticksCatchable > 0)
                    {
                        this.motionY -= (double)(this.rand.nextFloat() * this.rand.nextFloat() * this.rand.nextFloat()) * 0.2D;
                    }
                }

                d2 = d10 * 2.0D - 1.0D;
                this.motionY += 0.03999999910593033D * d2;

                if (d10 > 0.0D)
                {
                    f6 = (float)((double)f6 * 0.9D);
                    this.motionY *= 0.8D;
                }

                this.motionX *= (double)f6;
                this.motionY *= (double)f6;
                this.motionZ *= (double)f6;
                this.setPosition(this.posX, this.posY, this.posZ);
            }
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound tagCompound)
    {
        tagCompound.setShort("xTile", (short)this.xTile);
        tagCompound.setShort("yTile", (short)this.yTile);
        tagCompound.setShort("zTile", (short)this.zTile);
        ResourceLocation resourcelocation = (ResourceLocation)Block.blockRegistry.getNameForObject(this.inTile);
        tagCompound.setString("inTile", resourcelocation == null ? "" : resourcelocation.toString());
        tagCompound.setByte("shake", (byte)this.shake);
        tagCompound.setByte("inGround", (byte)(this.inGround ? 1 : 0));
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound tagCompund)
    {
        this.xTile = tagCompund.getShort("xTile");
        this.yTile = tagCompund.getShort("yTile");
        this.zTile = tagCompund.getShort("zTile");

        if (tagCompund.hasKey("inTile", 8))
        {
            this.inTile = Block.getBlockFromName(tagCompund.getString("inTile"));
        }
        else
        {
            this.inTile = Block.getBlockById(tagCompund.getByte("inTile") & 255);
        }

        this.shake = tagCompund.getByte("shake") & 255;
        this.inGround = tagCompund.getByte("inGround") == 1;
    }

    public int handleHookRetraction()
    {
        if (this.worldObj.isRemote)
        {
            return 0;
        }
        else
        {
            byte b0 = 0;

            if (this.caughtEntity != null)
            {
                double d0 = this.angler.posX - this.posX;
                double d2 = this.angler.posY - this.posY;
                double d4 = this.angler.posZ - this.posZ;
                double d6 = (double)MathHelper.sqrt_double(d0 * d0 + d2 * d2 + d4 * d4);
                double d8 = 0.1D;
                this.caughtEntity.motionX += d0 * d8;
                this.caughtEntity.motionY += d2 * d8 + (double)MathHelper.sqrt_double(d6) * 0.08D;
                this.caughtEntity.motionZ += d4 * d8;
                b0 = 3;
            }
            else if (this.ticksCatchable > 0)
            {
                EntityItem entityitem = new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, this.getFishingResult());
                double d1 = this.angler.posX - this.posX;
                double d3 = this.angler.posY - this.posY;
                double d5 = this.angler.posZ - this.posZ;
                double d7 = (double)MathHelper.sqrt_double(d1 * d1 + d3 * d3 + d5 * d5);
                double d9 = 0.1D;
                entityitem.motionX = d1 * d9;
                entityitem.motionY = d3 * d9 + (double)MathHelper.sqrt_double(d7) * 0.08D;
                entityitem.motionZ = d5 * d9;
                this.worldObj.spawnEntityInWorld(entityitem);
                this.angler.worldObj.spawnEntityInWorld(new EntityXPOrb(this.angler.worldObj, this.angler.posX, this.angler.posY + 0.5D, this.angler.posZ + 0.5D, this.rand.nextInt(6) + 1));
                b0 = 1;
            }

            if (this.inGround)
            {
                b0 = 2;
            }

            this.setDead();
            //this.angler.fishEntity = null;
            this.tracker.fishingEntity = null;
            return b0;
        }
    }

    @SuppressWarnings("unused")
    private ItemStack getFishingResult()
    {
        float f = this.worldObj.rand.nextFloat();
        int i = EnchantmentHelper.getLuckOfSeaModifier(this.angler);
        int j = EnchantmentHelper.getLureModifier(this.angler);


        if (true)
        {
        	((FishingSkill)this.tracker.GetSkillByID("fishing")).CatchEvent(this.angler,net.minecraftforge.common.FishingHooks.getFishableCategory(f, i, j));
        	this.angler.addStat(net.minecraftforge.common.FishingHooks.getFishableCategory(f, i, j).stat, 1);
            ItemStack item = net.minecraftforge.common.FishingHooks.getRandomFishable(this.rand, f, i, j);
            
            return item;
        }

        float f1 = 0.1F - (float)i * 0.025F - (float)j * 0.01F;
        float f2 = 0.05F + (float)i * 0.01F - (float)j * 0.01F;
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        f2 = MathHelper.clamp_float(f2, 0.0F, 1.0F);

        if (f < f1)
        {
            this.angler.triggerAchievement(StatList.junkFishedStat);
            return ((WeightedRandomFishable)WeightedRandom.getRandomItem(this.rand, JUNK)).getItemStack(this.rand);
        }
        else
        {
            f -= f1;

            if (f < f2)
            {
                this.angler.triggerAchievement(StatList.treasureFishedStat);
                return ((WeightedRandomFishable)WeightedRandom.getRandomItem(this.rand, TREASURE)).getItemStack(this.rand);
            }
            else
            {
                float f3 = f - f2;
                this.angler.triggerAchievement(StatList.fishCaughtStat);
                return ((WeightedRandomFishable)WeightedRandom.getRandomItem(this.rand, FISH)).getItemStack(this.rand);
            }
        }
    }

    /**
     * Will get destroyed next tick.
     */
    public void setDead()
    {
        super.setDead();

        if (this.angler != null)
        {
  //TODO had to put this in.
        	if(!this.worldObj.isRemote)
        	{
        		this.tracker.fishingEntity = null;
        	}
        }
    }

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		 String id = this.angler.getName();
		ByteBufUtils.writeUTF8String(buffer, id);
		
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) 
	{
		
		this.angler = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(ByteBufUtils.readUTF8String(additionalData));
	}
}