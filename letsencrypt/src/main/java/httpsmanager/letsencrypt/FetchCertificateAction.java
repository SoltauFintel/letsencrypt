package httpsmanager.letsencrypt;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;

public class FetchCertificateAction extends Action {

    @Override
    protected void execute() {
        Logger.info(getClass().getSimpleName());

        new LetsEncryptService().fetchCertificate();

        ctx.redirect("/");
    }
}
