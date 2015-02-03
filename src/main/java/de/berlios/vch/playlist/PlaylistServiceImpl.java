package de.berlios.vch.playlist;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Response;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.playlist.io.svdrp.CheckMplayerSvdrpInterface;
import de.berlios.vch.playlist.io.svdrp.CheckXineliboutputSvdrpInterface;
import de.berlios.vch.playlist.io.svdrp.CheckXinemediaplayerSvdrpInterface;

@Component
@Provides
public class PlaylistServiceImpl implements PlaylistService {

    private Playlist playlist = new Playlist();

    public static enum MediaPlayer {
        MPLAYER, XINEMEDIAPLAYER, XINELIBOUTPUT
    };

    public static MediaPlayer player = null;

    @Requires
    private LogService logger;

    @Requires
    private ConfigService config;

    private BundleContext ctx;

    private Preferences prefs;

    private Set<INetworkProtocol> protocols = new HashSet<INetworkProtocol>();

    public PlaylistServiceImpl(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

    @Override
    public void play(Playlist playlist, Map<String, String> requestPrefs) throws UnknownHostException, IOException, URISyntaxException {
        String svdrpHost = prefs.get("svdrp.host", "localhost");
        int svdrpPort = prefs.getInt("svdrp.port", 2001);

        if (requestPrefs != null) {
            if (requestPrefs.containsKey("svdrphost")) {
                svdrpHost = requestPrefs.get("svdrphost");
            }
            if (requestPrefs.containsKey("svdrpport")) {
                svdrpPort = Integer.parseInt(requestPrefs.get("svdrpport"));
            }
        }

        logger.log(LogService.LOG_INFO, "Starting media player plugin with SVDRP on " + svdrpHost + ":" + svdrpPort);
        org.hampelratte.svdrp.Connection svdrp = null;
        FileWriter fw = null;
        try {
            svdrp = new org.hampelratte.svdrp.Connection(svdrpHost, svdrpPort);
            File pls = File.createTempFile("vch_playlist_", ".pls");
            Command playCmd = getPlayCommand(svdrp, pls);
            fw = new FileWriter(pls);

            if (player == MediaPlayer.MPLAYER || player == MediaPlayer.XINEMEDIAPLAYER) {
                for (PlaylistEntry playlistEntry : playlist) {
                    // check, if we have to use a stream bridge for this format
                    String uri = bridgeIfNecessary(playlistEntry);

                    // write URI to playlist file
                    fw.write(uri + '\n');
                }
            } else if (player == MediaPlayer.XINELIBOUTPUT) {
                for (int i = 0; i < playlist.size(); i++) {
                    // check, if we have to use a stream bridge for this format
                    PlaylistEntry entry = playlist.get(i);
                    String uri = bridgeIfNecessary(entry);

                    fw.write("File" + (i + 1) + "=" + URLDecoder.decode(uri, "utf-8") + "\n");
                    fw.write("Title" + (i + 1) + "=" + entry.getVideo().getTitle() + '\n');
                }
            }
            fw.close();

            logger.log(LogService.LOG_DEBUG, "Trying to start playback with command: " + playCmd.getCommand());
            org.hampelratte.svdrp.Response resp = svdrp.send(playCmd);
            if (resp != null) {
                logger.log(LogService.LOG_DEBUG, "SVDRP response: " + resp.getCode() + " " + resp.getMessage());
                if (resp.getCode() < 900 || resp.getCode() > 999) {
                    throw new IOException(resp.getMessage().trim());
                }
            }
        } finally {
            if (svdrp != null) {
                svdrp.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }

    private String bridgeIfNecessary(PlaylistEntry playlistEntry) throws URISyntaxException {
        URI videoUri = playlistEntry.getVideo().getVideoUri();
        if ("file".equals(videoUri.getScheme())) {
            return videoUri.getPath();
        }

        for (INetworkProtocol proto : protocols) {
            String scheme = videoUri.getScheme();
            if (proto.getSchemes().contains(scheme)) {
                if (proto.isBridgeNeeded()) {
                    return proto.toBridgeUri(videoUri, playlistEntry.getVideo().getUserData()).toString();
                }
            }
        }
        return videoUri.toString();
    }

    private Command getPlayCommand(org.hampelratte.svdrp.Connection svdrp, final File playlistFile) throws IOException {
        Response res = svdrp.send(new CheckMplayerSvdrpInterface());
        if (res.getCode() == 214) {
            logger.log(LogService.LOG_DEBUG, "Using MPlayer to play the file");
            player = MediaPlayer.MPLAYER;
            return new Command() {
                @Override
                public String getCommand() {
                    return "plug mplayer play " + playlistFile.getAbsolutePath();
                }

                @Override
                public String toString() {
                    return "MPlayer PLAY";
                }
            };
        } else {
            res = svdrp.send(new CheckXineliboutputSvdrpInterface());
            if (res.getCode() == 214) {
                logger.log(LogService.LOG_DEBUG, "Using Xineliboutput to play the file");
                player = MediaPlayer.XINELIBOUTPUT;
                return new Command() {
                    @Override
                    public String getCommand() {
                        return "plug xineliboutput pmda " + playlistFile.getAbsolutePath();
                    }

                    @Override
                    public String toString() {
                        return "Xineliboutput PMDA";
                    }
                };
            } else {
                res = svdrp.send(new CheckXinemediaplayerSvdrpInterface());
                if (res.getCode() == 214) {
                    logger.log(LogService.LOG_DEBUG, "Using Xinemediaplayer to play the file");
                    player = MediaPlayer.XINEMEDIAPLAYER;
                    return new Command() {
                        @Override
                        public String getCommand() {
                            return "plug xinemediaplayer PLAYM3U " + playlistFile.getAbsolutePath();
                        }

                        @Override
                        public String toString() {
                            return "Xinemediaplayer PLAYM3U";
                        }
                    };
                } else {
                    throw new IOException("No media player plugin available");
                }
            }
        }
    }

    @Override
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    @Validate
    public void start() {
        prefs = config.getUserPreferences(ctx.getBundle().getSymbolicName());
    }

    @Bind(id = "protocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        protocols.add(protocol);
    }

    @Unbind(id = "protocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        protocols.remove(protocol);
    }
}
