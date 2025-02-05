package httpsmanager.base;

import github.soltaufintel.amalia.web.config.AppConfig;

public class Config {
    private final Folder html;
    private final Folder confd;
    private final Folder certificates;
    private final String dataFolder;
    private final String nginxImage;
    private final String nginxContainer;
    private final String nginxNetwork;
    private final String mailAddress;
    private final String certificateAuthorityUrl;
    
    public Config(AppConfig c) {
        html = new Folder(c, "html");
        confd = new Folder(c, "confd");
        certificates = new Folder(c, "certificates");
        
        dataFolder = c.get("data-folder", "/data");
        nginxImage = c.get("nginx.image", "nginx"); // better specify a version
        nginxContainer = c.get("nginx.container", "web");
        nginxNetwork = c.get("nginx.network"); // can be null
        certificateAuthorityUrl = c.get("certificate-authority.url", "acme://letsencrypt.org/staging");
        mailAddress = c.get("mail");
        if (mailAddress == null) {
            throw new RuntimeException("Please specify config 'mail' with a mail address for use by letsencrypt!");
        }
    }

    public Folder getHtml() {
        return html;
    }

    public Folder getConfd() {
        return confd;
    }

    public Folder getCertificates() {
        return certificates;
    }

    public String getDataFolder() {
        return dataFolder;
    }
    
    public String getNginxImage() {
        return nginxImage;
    }

    public String getNginxContainer() {
        return nginxContainer;
    }

    public String getNginxNetwork() {
        return nginxNetwork;
    }

    public String getMailAddress() {
        return mailAddress;
    }

    public String getCertificateAuthorityUrl() {
        return certificateAuthorityUrl;
    }
}
