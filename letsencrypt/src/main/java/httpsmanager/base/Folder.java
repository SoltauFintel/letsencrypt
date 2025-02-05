package httpsmanager.base;

import github.soltaufintel.amalia.web.config.AppConfig;

/**
 * This container needs to access nginx folders. They must be mapped onto the host.
 */
public class Folder {
    private final String key;
    /** folder in nginx container */
    private final String nginx;
    /** folder on host */
    private final String host;
    /** folder in this letsencrypt container */
    private final String local;

    public Folder(AppConfig config, String key) {
        this.key = key;
        nginx = config.get(key + "-folder.nginx");
        if (nginx == null) {
            throw new RuntimeException("Missing config option '" + key + "-folder.nginx'");
        }
        host = config.get(key + "-folder.host");
        if (host == null) {
            throw new RuntimeException("Missing config option '" + key + "-folder.host'");
        }
        local = config.get(key + "-folder.local");
        if (local == null) {
            throw new RuntimeException("Missing config option '" + key + "-folder.local'");
        }
    }

    public String getKey() {
        return key;
    }

    public String getNginx() {
        return nginx;
    }

    public String getHost() {
        return host;
    }

    public String getLocal() {
        return local;
    }
}
