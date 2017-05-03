package de.berlios.vch.playlist.io.svdrp;

import org.hampelratte.svdrp.Command;

public class CheckMpvSvdrpInterface extends Command {

    @Override
    public String getCommand() {
        return "plug mpv help play";
    }

    @Override
    public String toString() {
        return "MPV plugin PLAY test";
    }

}
