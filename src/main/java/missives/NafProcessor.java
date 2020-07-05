package missives;

import xjc.naf.LinguisticProcessors;
import xjc.naf.Lp;

import java.text.SimpleDateFormat;
import java.util.Date;

public interface NafProcessor {


    String getName();
    String getVersion();

    default LinguisticProcessors createLinguisticProcessors(String layer) {
        LinguisticProcessors lps = new LinguisticProcessors();
        Lp lp = new Lp();
        lp.setName(getName());
        lp.setVersion(getVersion());
        lp.setTimestamp(createTimestamp());
        lps.getLps().add(lp);
        lps.setLayer(layer);
        return lps;
    }

    static String createTimestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

}
