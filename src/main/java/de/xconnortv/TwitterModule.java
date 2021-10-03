package de.xconnortv;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.events.client.TickEvent;
import net.labymod.ingamegui.Module;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.ModuleCategoryRegistry;
import net.labymod.ingamegui.moduletypes.SimpleModule;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.NumberElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.support.util.Debug;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class TwitterModule extends SimpleModule {

    private static final ExecutorService SCHEDULER = Executors.newCachedThreadPool();

    private static final Pattern ILLEGAL_CHARACTER = Pattern.compile("[^\\w\\_]");

    private static final JsonParser PARSER = new JsonParser();

    private String errorMessage;

    private int followerCount;

    private int requestInterval;

    private String twitterName;

    private long lastRequest;

    public String getDisplayName() {
        return "Follower";
    }

    public String getDisplayValue() {
        return (this.errorMessage != null) ? this.errorMessage : NumberFormat.getInstance(Locale.US).format((this.followerCount < 0) ? 0L : this.followerCount);
    }

    public String getDefaultValue() {
        return "0";
    }

    public ControlElement.IconData getIconData() {
        return new ControlElement.IconData(new ResourceLocation("labymod/addons/labytwitter/twitter.png"));
    }

    public void loadSettings() {
        this.requestInterval = Integer.parseInt(getAttribute("interval", "30"));
        this.twitterName = parseTwitterName(getAttribute("twitterName", ""));
        this.followerCount = -1;
        if (this.requestInterval < 30)
            this.requestInterval = 30;
        if (this.twitterName != null)
            this.lastRequest = System.currentTimeMillis() + 1000L;
    }

    public void fillSubSettings(List<SettingsElement> settingsElements) {
        super.fillSubSettings(settingsElements);
        StringElement nameElement = (new StringElement(this, new ControlElement.IconData(Material.PAPER), "Twitter name", "twitterName")).maxLength(15);
        NumberElement updateElement = (new NumberElement(this, new ControlElement.IconData(Material.WATCH), "Update interval", "interval")).setRange(30, 500).addCallback(value -> TwitterModule.this.lastRequest = System.currentTimeMillis() + value.intValue() * 1000L);
        nameElement.setDescriptionText("Twitter name with or without at-sign (@)");
        updateElement.setDescriptionText("Update interval in seconds");
        settingsElements.add(nameElement);
        settingsElements.add(updateElement);
    }

    public String getSettingName() {
        return "Twitter Follower";
    }

    public String getDescription() {
        return "Shows any user's Twitter followers";
    }

    public ModuleCategory getCategory() {
        return ModuleCategoryRegistry.CATEGORY_EXTERNAL_SERVICES;
    }

    public int getSortingId() {
        return 0;
    }

    private String parseTwitterName(String input) {
        if (input.startsWith("@"))
            input = input.substring(1);
        if (input.isEmpty()) {
            this.errorMessage = "No username given.";
            return null;
        }
        if (input.length() > 15) {
            this.errorMessage = "Twitter name too long (1 - 15 chars)";
            return null;
        }
        if (ILLEGAL_CHARACTER.matcher(input).find()) {
            this.errorMessage = "Invalid username given. Provide it like @official_s_f";
            return null;
        }
        return input;
    }

    @Override
    public String getControlName() {
        return "LabyTwitter";
    }

    @Subscribe
    public void onClientTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.POST)
            return;
        if (getEnabled().size() != 0 && LabyMod.getInstance().isInGame() && isShown() && isDrawn() &&
                System.currentTimeMillis() > this.lastRequest) {
            this.lastRequest = System.currentTimeMillis() + this.requestInterval * 1000L;
            if (this.twitterName != null)
                SCHEDULER.execute(() -> {
                    try {
                        if (TwitterModule.this.followerCount < 0)
                            TwitterModule.this.errorMessage = "Loading followers...";
                        JsonArray object = TwitterModule.PARSER.parse(IOUtils.toString(new URL("https://cdn.syndication.twimg.com/widgets/followbutton/info.json?screen_names=" + TwitterModule.this.twitterName))).getAsJsonArray();
                        Debug.log(Debug.EnumDebugMode.ADDON, "twitter = " + object);
                        if (object.size() != 1) {
                            TwitterModule.this.errorMessage = "No user found.";
                            return;
                        }
                        TwitterModule.this.followerCount = object.get(0).getAsJsonObject().get("followers_count").getAsInt();
                        TwitterModule.this.errorMessage = null;
                    } catch (IOException e) {
                        TwitterModule.this.errorMessage = "Request failed";
                        e.printStackTrace();
                    }
                });
        }
    }
}
