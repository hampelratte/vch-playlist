package de.berlios.vch.playlist.io.svdrp;

import org.hampelratte.svdrp.Command;

public class CheckXinemediaplayerSvdrpInterface extends Command {

    @Override
    public String getCommand() {
        return "plug xinemediaplayer help PLAYM3U";
    }

    @Override
    public String toString() {
        return "xinemediaplayer plugin PLAYM3U test";
    }

}
