package httpsmanager.nginx;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;

public class InstallNginxAction extends Action {

    @Override
    protected void execute() {
        Logger.info(getClass().getSimpleName());
        new NginxService().install();
        ctx.redirect("/");
    }
}
