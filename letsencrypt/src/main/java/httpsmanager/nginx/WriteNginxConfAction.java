package httpsmanager.nginx;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;

public class WriteNginxConfAction extends Action {

    @Override
    protected void execute() {
        int phase = Integer.parseInt(ctx.pathParam("phase"));
        
        Logger.info("Write nginx default.conf phase " + phase + ". You must reinstall nginx after that.");
        
        new NginxService().writeConf(phase);
        
        ctx.redirect("/");
    }
}
