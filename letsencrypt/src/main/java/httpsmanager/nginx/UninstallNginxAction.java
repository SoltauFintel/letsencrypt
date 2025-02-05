package httpsmanager.nginx;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;

public class UninstallNginxAction extends Action {

    @Override
    protected void execute() {
        Logger.info(getClass().getSimpleName());
        new NginxService().uninstall();
        ctx.redirect("/");
    }
}
