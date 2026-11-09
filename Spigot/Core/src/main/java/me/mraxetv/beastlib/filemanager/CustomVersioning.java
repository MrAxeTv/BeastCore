package me.mraxetv.beastlib.filemanager;

import dev.dejvokep.boostedyaml.dvs.Pattern;
import dev.dejvokep.boostedyaml.dvs.segment.Segment;
import dev.dejvokep.boostedyaml.dvs.versioning.AutomaticVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import org.jetbrains.annotations.NotNull;

public class CustomVersioning extends AutomaticVersioning {

    public static final Pattern PATTERN = new Pattern(Segment.range(1, Integer.MAX_VALUE), Segment.literal("."), Segment.range(0, 10));

    public CustomVersioning(@NotNull String route) {
        super(PATTERN, route);
    }
}
