package httpsmanager.letsencrypt;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;

import httpsmanager.HttpsManager2App;
import httpsmanager.base.FileService;
import httpsmanager.domain.Domain;
import httpsmanager.domain.DomainAccess;
import httpsmanager.letsencrypt.acme4j.Acme4jRequest;
import httpsmanager.nginx.NginxService;

public class LetsEncryptService {

    public void fetchCertificate() {
        try {
            Acme4jRequest req = makeRequest();
            req.fetchCertificate();
            saveDomainFiles(req);
            new NginxService().updateNginx(2);
            Logger.info("---- LetsEncryptService completed ----");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Acme4jRequest makeRequest() {
        Acme4jRequest req = new Acme4jRequest() {
            private final List<File> files = new ArrayList<>();
            
            @Override
            public String getCertificateAuthorityUrl() {
                return HttpsManager2App.config.getCertificateAuthorityUrl();
            }
            
            @Override
            public Duration getTimeout() {
                return Duration.ofMinutes(4l);
            }
            
            @Override
            public String getMailAddress() {
                return HttpsManager2App.config.getMailAddress();
            }
            
            @Override
            public List<String> getDomains() {
                List<String> ret = new ArrayList<>();
                List<Domain> domains = new DomainAccess().list();
                for (Domain d : domains) {
                    if (d.isRoot()) {
                        ret.add(d.getPublicDomain()); // root domain at first position
                    }
                }
                for (Domain d : domains) {
                    if (!d.isRoot()) {
                        ret.add(d.getPublicDomain());
                    }
                }
                Logger.info("domains: " + ret);
                return ret;
            }
            
            @Override
            public File getUserKeyFile() {
                return file("user.key");
            }

            @Override
            public File getDomainKeyFile() {
                return file("domain.key");
            }
            
            @Override
            public File getDomainChainFile() {
                return file("domain-chain.crt");
            }
            
            private File file(String dnt) {
                String f = getCertificateAuthorityUrl().contains("staging") ? "/staging/" : "/production/";
                return new File(HttpsManager2App.config.getDataFolder() + f + dnt);
            }
            
            @Override
            public void acceptChallenge(String domain, String filename, String content) {
                File file = new File(HttpsManager2App.config.getHtml().getLocal() + "/acme/" + domain + filename);
                FileService.saveTextFile(file, content);
                if (file.isFile()) {
                    Logger.info(domain + " | acceptChallenge() saved file: " + file.getAbsolutePath());
                }
                files.add(file);
            }

            @Override
            public void allChallengesAccepted() {
                new NginxService().updateNginx(1);
            }
        };
        return req;
    }

    private void saveDomainFiles(Acme4jRequest req) {
        final String basePath = HttpsManager2App.config.getCertificates().getLocal() + "/";
        for (Domain domain : new DomainAccess().list()) {
            String path = basePath + domain.getPublicDomain() + "/";
            // TODO Die Dateien sind für alle Subdomains gleich. D.h. in Phase 2 könnte das auf eine zentrale Datei verweisen. (also ohne $publicDomain)
            copy(req.getDomainChainFile(), path + "fullchain.pem");
            copy(req.getDomainKeyFile(), path + "privkey.pem");
        }
    }
    
    private void copy(File sourceFile, String targetFile) {
        File tf = new File(targetFile);
        Logger.debug("copying '" + sourceFile.getAbsolutePath() + "' to '" + tf.getAbsolutePath() + "'");
        FileService.copy(sourceFile, tf);
    }
}
