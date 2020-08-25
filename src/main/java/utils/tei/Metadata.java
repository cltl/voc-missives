package utils.tei;

import utils.common.AbnormalProcessException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xjc.teiAll.Date;
import xjc.teiAll.Idno;
import xjc.teiAll.TEI;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Metadata {
    String documentTitle;
    String documentId;
    String collectionId;
    String date;
    private static Pattern datePattern = Pattern.compile("[0-9]+ [a-z\\.]+ 1[0-9]+");
    private static Pattern dayPattern = Pattern.compile("[\\D][1-3]*[0-9][\\D]");
    private static Pattern monthPattern = Pattern.compile("[a-z\\.]+");
    private static Pattern yearPattern = Pattern.compile("1[67][0-9][0-9]");

    private static HashMap<String,String> months = createMonthsMap();
    public static final Logger logger = LogManager.getLogger(Metadata.class);

    private static HashMap<String,String> createMonthsMap() {
        HashMap<String,String> map = new HashMap<>();
        map.put("jan", "01");
        map.put("feb", "02");
        map.put("maa", "03");
        map.put("apr", "04");
        map.put("mei", "05");
        map.put("jun", "06");
        map.put("jul", "07");
        map.put("aug", "08");
        map.put("sep", "09");
        map.put("oct", "10");
        map.put("okt", "10");
        map.put("nov", "11");
        map.put("dec", "12");
        return map;
    }

    public Metadata(String title, String collId, String docId, String date) {
        this.documentTitle = title;
        this.documentId = docId;
        this.collectionId = collId;
        this.date = date;
    }


    public static Metadata create(TEI tei) throws AbnormalProcessException {
        try {
            xjc.teiAll.FileDesc header = tei.getTeiHeader().getFileDesc();
            String title = (String) (header.getTitleStmt().getTitles().get(0)).getContent().get(0);
            List<Object> ids = header.getPublicationStmt().getPSAndAbs().get(0).getContent();
            String date = "";
            List<Object> dates = ids.stream().filter(x -> x instanceof Date).collect(Collectors.toList());
            if (! dates.isEmpty()) {
                try {
                    List<Object> d = ((Date) dates.get(0)).getContent();
                    if (!d.isEmpty()) {
                        String rawDate = (String) d.get(0);
                        if (rawDate.equals(""))
                            logger.info("Empty date for file " + getId(ids, "pid") + ", " + getId(ids, "sourceID"));
                        else {
                            date = formatDate(rawDate);
                            if (date.equals("")) {
                                date = formatDate(title);
                                if (date.equals(""))
                                    logger.info("Found non-conform date: " + rawDate + " for file " + getId(ids, "pid") + ", " + getId(ids, "sourceID") + "\n" + title);
                            }
                        }
                    } else
                        logger.info("Empty date for file " + getId(ids, "pid") + ", " + getId(ids, "sourceID"));
                } catch (IllegalArgumentException e) {
                    throw new AbnormalProcessException("Cannot asBaseToken date", e);
                }
            } else
                logger.info("Found no date for file " + getId(ids, "pid") + ", " + getId(ids, "sourceID"));
            return new Metadata(title, getId(ids, "sourceID"), getId(ids, "pid"), date);
        } catch (NullPointerException e) {
            throw new AbnormalProcessException("no FileDesc", e);
        }

    }

    private static String getId(List<Object> ids, String pid) {
        Idno idno = (Idno) ids.stream().filter(x -> x instanceof Idno && ((Idno) x).getType().equals(pid)).findFirst().orElse(null);
        return (String) idno.getContent().get(0);
    }

    public String getCollectionId() {
        return collectionId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }


    private static String formatDate(String date) throws IllegalArgumentException {
        if (date.equals(""))
            return date;
        Matcher m = datePattern.matcher(date);
        if (m.find()) {
            String[] elems = m.group().split(" ");
            String month;
            try {
                month = months.get(elems[1].substring(0, 3));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unknown month expression: " + elems[1]);
            }
            return elems[2] + "-" + month + "-" + formatDay(elems[0]);
        } else {
            return "";
        }
    }

    private static String getDateElts(String date) {
        StringBuilder sb = new StringBuilder();
        Matcher m = yearPattern.matcher(date);
        if (m.find())
            sb.append(m.group()).append("-");
        else
            sb.append("????-");
        m = monthPattern.matcher(date);
        if (m.find())
            sb.append(m.group()).append("-");
        else
            sb.append("??-");
        m = dayPattern.matcher(date);
        if (m.find())
            sb.append(m.group());
        else
            sb.append("??");
        return sb.toString();
    }

    private static String formatDay(String d) {
        return d.length() == 1 ? "0" + d : d;
    }


    public String getDate() {
        return date;
    }
}
