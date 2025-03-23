package httpsmanager.letsencrypt;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.pmw.tinylog.Logger;

import com.google.common.base.Strings;

import httpsmanager.HttpsManager2App;
import httpsmanager.base.FileService;
import httpsmanager.domain.Domain;
import httpsmanager.domain.DomainAccess;
import httpsmanager.letsencrypt.acme4j.Acme4jRequest;
import httpsmanager.nginx.NginxService;

public class LetsEncryptService {

    public void fetchCertificate() {
        try {
			Logger.info("==== LetsEncryptService ====");
			Logger.info("LetsEncryptService step 1: make requests");
			List<Acme4jRequest> requests = makeRequests();

			for (Acme4jRequest req : requests) {
				Logger.info("LetsEncryptService step 2: fetch certificate | " + req.getDomains().get(0));
				req.fetchCertificate();

				Logger.info("LetsEncryptService step 3: save domain files | " + req.getDomains().get(0));
				saveDomainFiles(req);
			}

			Logger.info("LetsEncryptService step 4: update Nginx (phase 2)");
			new NginxService().updateNginx(2);

			Logger.info("==== LetsEncryptService completed ====");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	private List<Acme4jRequest> makeRequests() {
		List<Acme4jRequest> ret = new ArrayList<>();
		List<Domain> domains = new DomainAccess().list();
		Set<String> certificateNames = new TreeSet<>();
		for (Domain domain : domains) {
			certificateNames.add(domain.getCertificateName());
		}
		Logger.info("certificate names: " + certificateNames);
		for (String certificateName : certificateNames) {
			ret.add(makeRequest(domains.stream()
					.filter(i -> i.getCertificateName().equals(certificateName))
					.collect(Collectors.toList())));
		}
		return ret;
	}
    
    private Acme4jRequest makeRequest(List<Domain> domains) {
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
                return new File(HttpsManager2App.config.getDataFolder() + "/" + domains.get(0).getCertificateName() + f + dnt);
            }
            
            @Override
            public void acceptChallenge(String domain, String filename, String content) {
                File file = new File(HttpsManager2App.config.getHtml().getLocal() + "/acme/" + domain + filename);
                FileService.saveTextFile(file, content);
                if (file.isFile()) {
                    Logger.info(Strings.padEnd(domain, 35, ' ') + " | acceptChallenge() saved file: " + file.getAbsolutePath());
                }
                files.add(file);
            }

            @Override
            public void allChallengesAccepted() {
                Logger.info("all challenges accepted -> update Nginx (phase 1)");
                new NginxService().updateNginx(1);
            }
        };
        Logger.info("CERTIFICATE NAME: " + domains.get(0).getCertificateName());
        Logger.info("  domains          : " + req.getDomains());
        Logger.info("  domain key file  : " + req.getDomainKeyFile());
        Logger.info("  user key file    : " + req.getUserKeyFile());
        Logger.info("  domain chain file: " + req.getDomainChainFile());
        return req;
    }

    private void saveDomainFiles(Acme4jRequest req) {
        final String basePath = HttpsManager2App.config.getCertificates().getLocal() + "/";
		for (Domain domain : new DomainAccess().list()) {
			if (!req.getDomains().contains(domain.getPublicDomain())) {
				continue;
			}
			Logger.info("  saving domain files for " + domain.getPublicDomain());
			String path = basePath + domain.getPublicDomain() + "/";
			copy(req.getDomainChainFile(), path + "fullchain.pem");
			copy(req.getDomainKeyFile(), path + "privkey.pem");
		}
	}
    
    private void copy(File sourceFile, String targetFile) {
        File tf = new File(targetFile);
        Logger.info("    copying '" + sourceFile.getAbsolutePath() + "' to '" + tf.getAbsolutePath() + "'");
        FileService.copy(sourceFile, tf);
    }
}
