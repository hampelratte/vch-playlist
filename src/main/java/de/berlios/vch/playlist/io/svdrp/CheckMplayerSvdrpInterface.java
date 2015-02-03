package de.berlios.vch.playlist.io.svdrp;

import org.hampelratte.svdrp.Command;

public class CheckMplayerSvdrpInterface extends Command {

    @Override
    public String getCommand() {
        return "plug mplayer help play";
    }

    @Override
    public String toString() {
        return "MPlayer plugin PLAY test";
    }

}
