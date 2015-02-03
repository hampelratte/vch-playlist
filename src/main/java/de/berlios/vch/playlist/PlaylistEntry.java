package de.berlios.vch.playlist;

import java.io.Serializable;
import java.util.UUID;

import de.berlios.vch.parser.IVideoPage;

public class PlaylistEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private String id;
    private IVideoPage video;

    public PlaylistEntry(IVideoPage video) {
        super();
        this.id = UUID.randomUUID().toString();
        this.video = video;
    }

    public IVideoPage getVideo() {
        return video;
    }
    
    public String getId() {
        return id;
    }
}
