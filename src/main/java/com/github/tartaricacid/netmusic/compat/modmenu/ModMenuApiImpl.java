package com.github.tartaricacid.netmusic.compat.modmenu;

import com.github.tartaricacid.netmusic.config.NetMusicConfigs;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

public class ModMenuApiImpl implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> NetMusicConfigs.getInstance().getConfigScreen(parent);
	}
}
