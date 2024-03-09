package com.liu;

import net.minecraft.util.math.BlockPos;

public class Tool2 extends Tool {
    public Tool2(Settings settings) {
        super(settings);
    }

    @Override
    protected void doBlockPos(BlockPos pos) {
        Tool.pos2 = pos;
    }


}
