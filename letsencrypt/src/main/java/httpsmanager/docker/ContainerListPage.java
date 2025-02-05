package httpsmanager.docker;

import java.util.List;

import org.pmw.tinylog.Logger;

import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManager2App;

/**
 * Gibt die Namen aller laufenden Docker Container aus.
 * Das ist mehr oder weniger eigentlich dafür da zu prüfen, ob überhaupt Zugriff auf Docker möglich ist.
 */
public class ContainerListPage extends Page {

    @Override
    protected void execute() {
        Logger.info("ContainerListPage");
        
        List<String> containerNames = HttpsManager2App.docker.getContainerNames(true);

        put("title", "Docker Container");
        DataList list = list("containers");
        for (String name : containerNames) {
            DataMap map = list.add();
            boolean r = name.endsWith(", running");
            map.put("isRunning", r);
            if (r) {
                name = name.substring(0, name.length() - ", running".length());
            }
            boolean x = name.endsWith(", exited");
            map.put("isExited", x);
            if (x) {
                name = name.substring(0, name.length() - ", exited".length());
            }
            map.put("name", esc(name));
        }
    }
}
