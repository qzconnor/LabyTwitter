package de.xconnortv;

import net.labymod.api.LabyModAddon;
import net.labymod.settings.elements.SettingsElement;

import java.util.List;

public class LabyTwitter extends LabyModAddon {
    @Override
    public void onEnable() {
        TwitterModule module = new TwitterModule();
        this.api.getEventService().registerListener(module);
        this.api.registerModule(module);
    }

    @Override
    public void loadConfig() {

    }

    @Override
    protected void fillSettings(List<SettingsElement> list) {

    }
}
