package httpsmanager.nginx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.pmw.tinylog.Logger;

import httpsmanager.HttpsManager2App;
import httpsmanager.base.FileService;
import httpsmanager.base.Folder;
import httpsmanager.base.StringTable;
import httpsmanager.docker.AbstractDocker.DockerVolume;
import httpsmanager.domain.Domain;
import httpsmanager.domain.DomainAccess;

public class NginxService {

    public void install() {
        String image = HttpsManager2App.config.getNginxImage();
        String name = HttpsManager2App.config.getNginxContainer();
        String network = HttpsManager2App.config.getNginxNetwork();
        Logger.info("Installing nginx container | image=" + image + " | ports=80,443 | container=" + name
                + (network == null ? "" : (" | network=" + network)));

        List<Folder> folders = List.of(HttpsManager2App.config.getHtml(), HttpsManager2App.config.getConfd(),
                HttpsManager2App.config.getCertificates());
        StringTable s = new StringTable();
        s.row("type", "html folder", "host folder", "local folder");
        folders.forEach(f -> s.row(f.getKey(), f.getNginx(), f.getHost(), f.getLocal()));
        Logger.info("Folders:\n" + s.toString());
        List<DockerVolume> volumes = folders.stream().map(f -> volume(f)).toList();

        HttpsManager2App.docker.createAndStartContainer(image, name, "80,443", network, volumes);
        
        Logger.info("  nginx container installed");
    }

    private DockerVolume volume(Folder folder) {
        return new DockerVolume(folder.getHost(), folder.getNginx());
    }
    
    public void uninstall() {
        Logger.debug("Uninstalling nginx container");
        HttpsManager2App.docker.removeContainer(HttpsManager2App.config.getNginxContainer(), Boolean.TRUE);
        Logger.info("nginx container uninstalled");
    }
    
    public void updateNginx(int phase) {
        Logger.info("-- update nginx, phase " + phase);
        writeConf(phase);
        uninstall();
        install();
    }
    
    public void writeConf(int phase) {
        writeDefaultConf(phase, HttpsManager2App.config.getConfd().getLocal(), new DomainAccess().list());
    }
    
    private void writeDefaultConf(int phase, String confdFolder, List<Domain> domains) {
        File file = new File(confdFolder + "/default.conf");
        String content = domains.stream().map(d ->
            getDefaultConfText(phase, d.isRoot())
                .replace("$publicDomain", d.getPublicDomain())
                .replace("$internalDomain", d.getInternalDomain())
                .replace("$certificateName", d.getCertificateName()) + "\n").collect(Collectors.joining());
        Logger.debug(content);
        file.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        } catch (IOException e) {
            throw new RuntimeException("Error writing file " + file.getAbsolutePath(), e);
        }
        Logger.info("## Saved file '" + file.getAbsolutePath() + "' for phase " + phase + " ##");
    }
    
    /**
     * @param phase 0: kein https Support, 1: Antragsphase, 2: https fertig Phase
     * @param isRootDomain -
     * @return part of default.conf
     */
    private String getDefaultConfText(int phase, boolean isRootDomain) {
        // TO-DO vermutlich muss ich diese Texte noch änderbar machen
        if (phase == 0) {
            if (isRootDomain) {
                return """
                        server {
                          listen      80;
                          server_name localhost;
                          index index.html index.htm;
                          location / {
                            $internalDomain;
                          }
                        }
                        """;
            } else {
                return """
                        server {
                          listen      80;
                          server_name $publicDomain;
                          location / {
                            proxy_pass http://$internalDomain;
                          }
                        }
                        """;
            }
        } else if (phase == 1) {
            if (isRootDomain) {
                return """
                        server {
                          listen      80;
                          server_name localhost;
                          index index.html index.htm;
                          location / {
                            $internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /usr/share/nginx/html/acme/$publicDomain;
                          }
                        }
                        """;
            } else {
                return """
                        server {
                          listen      80;
                          server_name $publicDomain;
                          location / {
                            proxy_pass http://$internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /usr/share/nginx/html/acme/$publicDomain;
                          }
                        }
                        """;
            }
        } else if (phase == 2) {
            if (isRootDomain) {
                return """
                        server {
                          listen      80;
                          server_name localhost;
                          location ~ /.well-known/acme-challenge/ {
                            root /usr/share/nginx/html/acme/$publicDomain;
                          }
                          return 301 https://$publicDomain$request_uri;
                        }
                        server {
                          listen      443 ssl http2;
                          ssl_certificate     /etc/letsencrypt/live/$certificateName/fullchain.pem;
                          ssl_certificate_key /etc/letsencrypt/live/$certificateName/privkey.pem;
                          server_name $publicDomain;
                          index index.html index.htm;
                          location / {
                            $internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /usr/share/nginx/html/acme/$publicDomain;
                          }
                        }
                        """;
            } else {
                return """
                        server {
                          listen      80;
                          server_name $publicDomain;
                          location ~ /.well-known/acme-challenge/ {
                            root /usr/share/nginx/html/acme/$publicDomain;
                          }
                          return 301 https://$publicDomain$request_uri;
                        }
                        server {
                          listen      443 ssl http2;
                          ssl_certificate     /etc/letsencrypt/live/$certificateName/fullchain.pem;
                          ssl_certificate_key /etc/letsencrypt/live/$certificateName/privkey.pem;
                          server_name $publicDomain;
                          location / {
                            proxy_pass http://$internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /usr/share/nginx/html/acme/$publicDomain;
                          }
                        }
                        """;
            }
        } else {
            throw new RuntimeException("Unsupported phase");
        }
    }
    
    public void cleanupAcmeFiles() {
        File acme = new File(HttpsManager2App.config.getHtml().getLocal() + "/acme");
        if (acme.isDirectory()) {
            FileService.deleteFolder(acme);
            Logger.info("Folder deleted: " + acme.getAbsolutePath());
        }
    }
}
