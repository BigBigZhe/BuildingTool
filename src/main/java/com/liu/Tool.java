package com.liu;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class Tool extends Item {

    public static BlockPos pos1, pos2;

    public Tool(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient) {
            doBlockPos(context.getBlockPos());
        }
        return ActionResult.PASS;
    }

    protected abstract void doBlockPos(BlockPos pos);
}
