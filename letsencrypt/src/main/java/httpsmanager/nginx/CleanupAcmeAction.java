package httpsmanager.nginx;

import github.soltaufintel.amalia.web.action.Action;

public class CleanupAcmeAction extends Action {

    @Override
    protected void execute() {
        new NginxService().cleanupAcmeFiles();
        ctx.redirect("/");
    }
}
