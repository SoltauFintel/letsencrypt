package httpsmanager.letsencrypt.acme4j;

public interface AcceptChallenge {

    /**
     * Create file (filename) with content (content) for domain http://(domain) in webserver.
     */
    void acceptChallenge(String domain, String filename, String content);
    
    /**
     * It's time to restart web server
     */
    void allChallengesAccepted();
}
