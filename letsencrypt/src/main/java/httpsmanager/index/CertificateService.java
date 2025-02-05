package httpsmanager.index;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.pmw.tinylog.Logger;

import httpsmanager.domain.Domain;
import httpsmanager.domain.DomainAccess;

public class CertificateService {
    private String certNotAfterDate;
    
    public List<DomainAndCertificate> getDomainAndCertificates() {
        return new DomainAccess().list().stream().map(domain -> checkCertificate(domain)).toList();
    }
    
    private DomainAndCertificate checkCertificate(Domain d) {
        DomainAndCertificate ret = new DomainAndCertificate(d);
        String state = "?";
        try {
            state = checkHttpsUrl("https://" + d.getPublicDomain(), true, true);
            ret.setOk(state != null && state.startsWith("ok"));
        } catch (Exception e) {
            state = e.getMessage();
        }
        if (state.contains("401") || state.contains("500")) {
            try {
                state = checkHttpsUrl("https://" + d.getPublicDomain() + "/rest/_ping", true, true) + " (/rest/_ping)";
                ret.setOk(true);
            } catch (Exception e) {
                state = e.getMessage();
            }
        }
        ret.setState(state);
        return ret;
    }

    private String checkHttpsUrl(String url, boolean letsEncrypt, boolean okResponse) throws Exception {
        certNotAfterDate = "?";
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> {
            boolean found = false;
            for (X509Certificate c : cert) {
                // most important test:
                // Should never fail for letsencrypt certificates because certbot is in cron.d
                // and runs ~every month.
                if (expired(c)) {
                    throw new RuntimeException("Certificate expires soon!\nissuer: "
                            + c.getIssuerX500Principal().getName() + "\nURL: " + url);
                }

                if (letsEncrypt && c.getIssuerX500Principal().getName().toLowerCase().contains("let's encrypt")) {
                    found = true;
                    certNotAfterDate = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(c.getNotAfter());
                    // RenewalTimer.registerDay(c.getNotAfter());
                    
                    Logger.info(c.getIssuerX500Principal().getName() + ", " + certNotAfterDate);
                }
            }
            if (!found) {
                throw new RuntimeException("Check certificate issuer!n" + url + "\n");
            }
            return cert.length > 0;
        };
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslsf).build();
        BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(
                socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();

        URI uri = URI.create(url);
        HttpResponse response = httpClient.execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()),
                new HttpGet(url), HttpClientContext.create());
        int status = response.getStatusLine().getStatusCode();
        if (!(status >= 200 && status < 300)) {
            return "nicht ok: status " + status + (!"?".equals(certNotAfterDate) ? (", " + certNotAfterDate) : "");
        }
        return "ok, " + certNotAfterDate;
    }

    private boolean expired(X509Certificate cert) {
        LocalDateTime now1 = LocalDateTime.now().plusWeeks(1);
        Date now = Date.from(now1.atZone(ZoneId.systemDefault()).toInstant()); // give us 1 week time to react
        return cert.getNotAfter().before(now);
    }
    
    public static class DomainAndCertificate {
        private final Domain domain;
        private String state;
        private boolean ok;
        
        public DomainAndCertificate(Domain domain) {
            this.domain = domain;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }

        public Domain getDomain() {
            return domain;
        }
    }
}
