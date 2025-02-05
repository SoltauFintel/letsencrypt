package httpsmanager.letsencrypt.acme4j;

import java.io.File;
import java.time.Duration;
import java.util.List;

public interface Acme4jRequest extends AcceptChallenge {
    String PRODUCTION = "acme://letsencrypt.org";
    String STAGING = "acme://letsencrypt.org/staging";

    /**
     * Call acme4j.
     * After the successful call of this method:
     * Configure your web server to use the req.getDomainChainFile() and req.getDomainKeyFile() for the requested domains. 
     */
    default void fetchCertificate() {
        Acme4jService.fetchCertificate(
                getDomains(),
                getCertificateAuthorityUrl(),
                getMailAddress(),
                getUserKeyFile(),
                getDomainKeyFile(),
                getDomainChainFile(),
                (AcceptChallenge) this);
    }

    /**
     * @return PRODUCTION or STAGING 
     */
    String getCertificateAuthorityUrl();

    List<String> getDomains();

    String getMailAddress();
    
    /**
     * @return user.key
     */
    File getUserKeyFile();

    /**
     * @return domain.key
     */
    File getDomainKeyFile();

    /**
     * @return domain-chain.crt
     */
    File getDomainChainFile();
    
    /**
     * @return e.g. Duration.ofSeconds(60l)
     */
    Duration getTimeout();
}
