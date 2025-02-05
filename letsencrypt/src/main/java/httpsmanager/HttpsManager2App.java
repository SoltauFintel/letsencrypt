package httpsmanager;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.auth.simple.SimpleAuth;
import github.soltaufintel.amalia.web.builder.LoggingInitializer;
import github.soltaufintel.amalia.web.builder.WebAppBuilder;
import github.soltaufintel.amalia.web.route.PingRouteDefinition;
import github.soltaufintel.amalia.web.route.RouteDefinitions;
import httpsmanager.base.Config;
import httpsmanager.docker.AbstractDocker;
import httpsmanager.docker.ContainerListPage;
import httpsmanager.docker.UnixDocker;
import httpsmanager.docker.WindowsDocker;
import httpsmanager.domain.AddDomainPage;
import httpsmanager.domain.DeleteDomainAction;
import httpsmanager.domain.DomainListPage;
import httpsmanager.domain.EditDomainPage;
import httpsmanager.index.CheckCertificatesPage;
import httpsmanager.index.IndexPage;
import httpsmanager.letsencrypt.FetchCertificateAction;
import httpsmanager.nginx.CleanupAcmeAction;
import httpsmanager.nginx.InstallNginxAction;
import httpsmanager.nginx.ReinstallNginxAction;
import httpsmanager.nginx.UninstallNginxAction;
import httpsmanager.nginx.WriteNginxConfAction;
import spark.Spark;

public class HttpsManager2App extends RouteDefinitions {
    public static final String VERSION = "2.0.0";
    public static Config config;
    public static AbstractDocker docker;
    
    // TODO jetzt fehlt noch das laufende Renewal
    
    @Override
    public void routes() {
        get("/", IndexPage.class);
        get("/check-certificates", CheckCertificatesPage.class);
        Spark.get("/rest/_info", (req, res) -> "https-manager " + VERSION + ", certificate authority: " + config.getCertificateAuthorityUrl());

        // Let's encrypt
        get("/fetch-certificate", FetchCertificateAction.class);

        // Domains
        form("/domain/:id/edit", EditDomainPage.class);
        get("/domain/:id/delete", DeleteDomainAction.class);
        form("/domain/add", AddDomainPage.class);
        get("/domain", DomainListPage.class);
        
        // Docker
        get("/container", ContainerListPage.class);
        
        // nginx
        get("/install-nginx", InstallNginxAction.class);
        get("/reinstall-nginx", ReinstallNginxAction.class);
        get("/uninstall-nginx", UninstallNginxAction.class);
        get("/write-nginx-conf/:phase", WriteNginxConfAction.class);
        get("/cleanup-acme", CleanupAcmeAction.class);
    }
    
    public static void main(String[] args) {
        new WebAppBuilder(VERSION)
            .withLogging(new LoggingInitializer(Level.INFO, "{date} {level}  {message}"))
            .withAuth(config -> new SimpleAuth(config))
            .withTemplatesFolders(HttpsManager2App.class, "/templates")
            .withInitializer(cfg -> config = new Config(cfg))
            .withInitializer(cfg -> initDocker())
            .withInitializer(cfg -> Security.addProvider(new BouncyCastleProvider()))
            .withRoutes(new HttpsManager2App())
            .withRoutes(new PingRouteDefinition())
            .build()
            .boot();
    }
    
    public static void initDocker() {
        String os = System.getProperty("os.name");
        boolean isWindows = os != null && os.toLowerCase().contains("win");
        docker = isWindows ? new WindowsDocker() : new UnixDocker();
        Logger.debug(os + " -> " + docker.getClass().getSimpleName());
    }
}
