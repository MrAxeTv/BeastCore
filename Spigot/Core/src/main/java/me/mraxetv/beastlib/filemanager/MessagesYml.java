package me.mraxetv.beastlib.filemanager;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import org.bukkit.plugin.java.JavaPlugin;



import java.io.File;
import java.io.IOException;

public class MessagesYml<T extends JavaPlugin> {

    private T pl;
    private  YamlDocument ymlDocument;


    public MessagesYml(T pl){
        this.pl = pl;

        try {
            ymlDocument = YamlDocument.create(new File( pl.getDataFolder(),"messages.yml"),pl.getResource("messages.yml"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.builder().setEncoding(DumperSettings.Encoding.UNICODE).setScalarFormatter(
                            (tag, value, role, def) -> {
                                if(role == NodeRole.KEY) return ScalarStyle.PLAIN;
                                else { return tag == Tag.INT ? ScalarStyle.PLAIN : ScalarStyle.DOUBLE_QUOTED;}
                            }).build(),
                    UpdaterSettings.builder().setVersioning(new CustomVersioning("ConfigVersion"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public YamlDocument getConfig(){

        return ymlDocument;
    }

}
