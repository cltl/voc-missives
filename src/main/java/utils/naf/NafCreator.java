package utils.naf;

import xjc.naf.LinguisticProcessors;
import xjc.naf.Lp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


public interface NafCreator {
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


    default void addLinguisticProcessor(NafDoc naf, String layer) {
        List<LinguisticProcessors> lps = naf.getLinguisticProcessorsList();
        LinguisticProcessors lp = createLinguisticProcessors(layer);
        List<LinguisticProcessors> existing = lps.stream().filter(x -> x.getLayer().equals(layer)).collect(Collectors.toList());
        if (existing.isEmpty())
            lps.add(lp);
        else
            existing.get(0).getLps().addAll(lp.getLps());
    }

    static String createTimestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
}
