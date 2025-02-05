package httpsmanager.index;

import java.util.List;

import org.pmw.tinylog.Logger;

import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.index.CertificateService.DomainAndCertificate;

public class CheckCertificatesPage extends Page {

    @Override
    protected void execute() {
        Logger.info(getClass().getSimpleName());

        List<DomainAndCertificate> dac = new CertificateService().getDomainAndCertificates();

        put("title", "SSL Zertifikate aller Domains prüfen");
        DataList list = list("domains");
        dac.forEach(d -> {
            DataMap map = list.add();
            map.put("name", esc(d.getDomain().getPublicDomain()));
            map.put("state", esc(d.getState()));
            map.put("ok", d.isOk());
        });
    }

}
