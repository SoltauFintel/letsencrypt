package httpsmanager.letsencrypt.acme4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.pmw.tinylog.Logger;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Problem;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;

/**
 * Call acme4j with http-01 challenge.
 */
public class Acme4jService {
    public static Duration timeout = Duration.ofMinutes(4l);
    
    public static void fetchCertificate(List<String> domains, String CertificateAuthorityUrl, String mailAddress,
            File userKeyFile, File domainKeyFile, File domainChainFile, AcceptChallenge acceptChallenge) {
        try {
            Order order = createOrder(domains, CertificateAuthorityUrl, mailAddress, userKeyFile);
            performAllRequiredAuthorizations(order, acceptChallenge);
            waitForOrderCompletion(order, domains, domainKeyFile, domainChainFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Order createOrder(List<String> domains, String certificateAuthorityUrl, String mail, File userKeyFile) throws AcmeException, IOException {
        KeyPair userKeyPair = loadOrCreateKeyPair(userKeyFile, () -> KeyPairUtils.createKeyPair());
        Session session = new Session(certificateAuthorityUrl);
        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(userKeyPair)
                .addEmail(mail)
                .create(session);
        Logger.debug("location: " + account.getLocation());
        Order order = account.newOrder().domains(domains).create();
        Logger.debug("order created");
        return order;
    }
    
    private static void performAllRequiredAuthorizations(Order order, AcceptChallenge ac) throws AcmeException, InterruptedException {
        Logger.debug("performAllRequiredAuthorizations part 1");
        List<AuthorizationChallengePair> pairs = new ArrayList<>();
        for (Authorization auth : order.getAuthorizations()) {
            AuthorizationChallengePair pair = new AuthorizationChallengePair();
            pair.challenge = authorize1(auth, ac);
            if (pair.challenge != null) {
                pair.authorization = auth;
                pairs.add(pair);
            }
        }

        Logger.debug("performAllRequiredAuthorizations part 2");
        ac.allChallengesAccepted();

        Logger.debug("performAllRequiredAuthorizations part 3");
        for (AuthorizationChallengePair pair : pairs) {
            authorize3(pair.authorization, ac, pair.challenge);
        }
        Logger.debug("performAllRequiredAuthorizations end");
    }
    
    private static class AuthorizationChallengePair {
        Authorization authorization;
        Challenge challenge;
    }

    private static Challenge authorize1(Authorization auth, AcceptChallenge ac) throws AcmeException, InterruptedException {
        String domain = auth.getIdentifier().getDomain();
        if (Status.VALID.equals(auth.getStatus())) {
            Logger.info("Authorization for domain {} is already valid. No need to process a challenge.", domain);
            return null;
        }
        Logger.info("Authorization for domain {}", domain);
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class).orElseThrow(
                () -> new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do..."));
        ac.acceptChallenge(auth.getIdentifier().getDomain(), "/.well-known/acme-challenge/" + challenge.getToken(), challenge.getAuthorization());
        return challenge;
    }

    private static void authorize3(Authorization auth, AcceptChallenge ac, Challenge challenge) throws AcmeException, InterruptedException {
        // If the challenge is already verified, there's no need to execute it again.
        if (Status.VALID.equals(challenge.getStatus())) {
            Logger.info("The challenge is already verified, there's no need to execute it again.");
            return;
        }

        // Now trigger the challenge.
        Logger.info("trigger challenge");
        challenge.trigger();

        // Poll for the challenge to complete.
        Logger.info("  waiting for challenge to be completed...");
        Status status = challenge.waitForCompletion(timeout);
        if (!Status.VALID.equals(status)) {
            Logger.error("Challenge has failed, reason: {}", challenge.getError().map(Problem::toString).orElse("unknown"));
            throw new AcmeException("Challenge failed. Giving up. See error log.");
        }
        
        Logger.info("  challenge completed");
    }

    private static void waitForOrderCompletion(Order order, List<String> domains, File domainKeyFile, File domainChainFile) throws IOException, AcmeException, InterruptedException {
        KeyPair domainKeyPair = loadOrCreateKeyPair(domainKeyFile, () -> KeyPairUtils.createKeyPair(4096));

        order.waitUntilReady(timeout); // Wait for the order to become READY
        order.execute(domainKeyPair); // Order the certificate

        // Wait for the order to complete
        Status status = order.waitForCompletion(timeout);
        if (status != Status.VALID) {
            Logger.error("Order has failed, reason: {}", order.getError().map(Problem::toString).orElse("unknown"));
            throw new AcmeException("Order failed. Giving up. See log.");
        }

        // Get the certificate
        Certificate certificate = order.getCertificate();

        Logger.info("++++ Success! The certificate for domains {} has been generated!", domains);
        Logger.info("              Certificate URL: {}", certificate.getLocation());

        // Write a combined file containing the certificate and chain.
        domainChainFile.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(domainChainFile)) {
            certificate.writeCertificate(w);
        }
    }

    private static KeyPair loadOrCreateKeyPair(File file, Supplier<KeyPair> keySupplier) throws IOException {
        if (file.exists()) {
            try (FileReader r = new FileReader(file)) {
                return KeyPairUtils.readKeyPair(r);
            }
        } else {
            KeyPair userKeyPair = keySupplier.get();
            file.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file)) {
                KeyPairUtils.writeKeyPair(userKeyPair, w);
            }
            return userKeyPair;
        }
    }
}
