package httpsmanager.index;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManager2App;

public class IndexPage extends Page {

    @Override
    protected void execute() {
        put("title", "https manager 2");
        put("VERSION", HttpsManager2App.VERSION);
    }
}
