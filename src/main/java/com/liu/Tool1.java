package com.liu;

import net.minecraft.util.math.BlockPos;

public class Tool1 extends Tool {
    public Tool1(Settings settings) {
        super(settings);
    }

    @Override
    protected void doBlockPos(BlockPos pos) {
        Tool.pos1 = pos;
    }


}
