package httpsmanager.nginx;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;

public class ReinstallNginxAction extends Action {

    @Override
    protected void execute() {
        Logger.info(getClass().getSimpleName());
        NginxService nginx = new NginxService();
        nginx.uninstall();
        nginx.install();
        ctx.redirect("/");
    }
}
