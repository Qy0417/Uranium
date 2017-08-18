package cc.uraniummc;

import org.bukkit.configuration.file.YamlConfiguration;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.cauldron.configuration.BoolSetting;
import net.minecraftforge.cauldron.configuration.ConfigBase;
import net.minecraftforge.cauldron.configuration.IntSetting;
import net.minecraftforge.cauldron.configuration.Setting;
import net.minecraftforge.cauldron.configuration.StringSetting;

public class UraniumConfig extends ConfigBase {
    public BoolSetting commandEnable = new BoolSetting(this, "command.enable",
            true, "Enable Uranium command");
    public BoolSetting updatecheckerEnable = new BoolSetting(this,
            "updatechecker.enable", false, "Enable Uranium update checker");
    public StringSetting updatecheckerSymlinks = new StringSetting(this,
            "updatechecker.symlinks", "Uranium.jar", "(Re)create symlinks after update");
    public BoolSetting updatecheckerAutoinstall = new BoolSetting(this,
            "updatechecker.autoinstall", false, "Install updates without confirming");
    public BoolSetting updatecheckerAutorestart = new BoolSetting(this,
            "updatechecker.autorestart", false, "Restart server after updating without confirming (set restart script in spigot.yml)");
    public BoolSetting updatecheckerQuiet = new BoolSetting(this,
            "updatechecker.quiet", false, "Print less info during update");

    public BoolSetting loggingMaterialInjection = new BoolSetting(this,
            "logging.materialInjection", false, "Log material injection event");
    public BoolSetting loggingClientModList = new BoolSetting(this,
            "logging.clientModList", true, "Print client's mod list during attempt to join");
        
    public BoolSetting commonAllowNetherPortal = new BoolSetting(this,
            "common.allowNetherPortalBesidesOverworld", false, "Allow nether portals in dimensions besides overworld");
    public BoolSetting commonFastLeavesDecayEnable = new BoolSetting(this,
            "common.fastLeavesDecay.enable", false, "Enable fast decaying of leaves, not affects drop chanches /etc");
    public IntSetting commonFastLeavesDecayMinTickTime = new IntSetting(this,
            "common.fastLeavesDecay.minTickTime", 5, "Minimal amount of tick between block updates");
    public IntSetting commonFastLeavesDecayMaxTickTime = new IntSetting(this,
            "common.fastLeavesDecay.maxTickTime", 10, "Minimal amount of tick between block updates");
    public IntSetting commonMaxChunkGenPerTick = new IntSetting(this,
            "common.maxChunkGenPerTick", 1, "How many chunks generate during tick");

    public BoolSetting experimentalTileEntityListRecreation = new BoolSetting(this,
            "experimental.tileEntityListRecreation", false, "EXPERIMENTAL! Recreate list of TE each tick.");    
    public static boolean tileEntityListRecreation;
    
    public UraniumConfig() {
        super("kcauldron.yml", "kc");
        register(commandEnable);
        register(updatecheckerEnable);
        register(updatecheckerSymlinks);
        register(updatecheckerAutoinstall);
        register(updatecheckerAutorestart);
        register(updatecheckerQuiet);
        register(loggingMaterialInjection);
        register(loggingClientModList);
        register(commonAllowNetherPortal);
        register(commonFastLeavesDecayEnable);
        register(commonFastLeavesDecayMinTickTime);
        register(commonFastLeavesDecayMaxTickTime);
        register(experimentalTileEntityListRecreation);
        load();
    }

    private void register(Setting<?> setting) {
        settings.put(setting.path, setting);
    }

    @Override
    public void registerCommands() {
        if (commandEnable.getValue()) {
            super.registerCommands();
        }
    }

    @Override
    protected void addCommands() {
        commands.put(commandName, new UraniumCommand(commandName));
    }

    @Override
    protected void load() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            String header = "";
            for (Setting<?> toggle : settings.values()) {
                if (!toggle.description.equals(""))
                    header += "Setting: " + toggle.path + " Default: "
                            + toggle.def + " # " + toggle.description + "\n";

                config.addDefault(toggle.path, toggle.def);
                settings.get(toggle.path).setValue(
                        config.getString(toggle.path));
            }
            config.options().header(header);
            config.options().copyDefaults(true);
            save();
        } catch (Exception ex) {
            MinecraftServer.getServer().logSevere(
                    "Could not load " + this.configFile);
            ex.printStackTrace();
        }
        tileEntityListRecreation = experimentalTileEntityListRecreation.getValue();
    }
}