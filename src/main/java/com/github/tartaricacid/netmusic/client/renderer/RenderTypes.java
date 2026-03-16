package com.github.tartaricacid.netmusic.client.renderer;

import net.xiaoyu233.fml.reload.utils.IdUtil;

public class RenderTypes {
    public static int cdBurnerRenderType = getNextRenderType();
    public static int computerRenderType = getNextRenderType();
    public static int musicPlayerRenderType = getNextRenderType();

    public static int getNextRenderType() {
        return IdUtil.getNextRenderType();
    }
}
