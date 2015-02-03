package de.berlios.vch.playlist;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;


public interface PlaylistService {
    public Playlist getPlaylist();
    public void setPlaylist(Playlist playlist);
    public void play(Playlist playlist, Map<String, String> requestPrefs) throws UnknownHostException, IOException, URISyntaxException;
}
