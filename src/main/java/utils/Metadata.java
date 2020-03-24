package utils;

import xjc.tei.FileDesc;
import xjc.tei.Idno;
import xjc.tei.TEI;

import java.util.List;

public class Metadata {
    String documentTitle;
    String documentId;
    String collectionId;

    public Metadata(String title, String collId, String docId) {
        this.documentTitle = title;
        this.documentId = docId;
        this.collectionId = collId;
    }

    public static Metadata create(TEI tei) {
        FileDesc header = tei.getTeiHeader().getFileDesc();
        String title = (String) (header.getTitleStmt().getTitles().get(0)).getContent().get(0);
        List<Object> ids = header.getPublicationStmt().getPS().get(0).getContent();
        return new Metadata(title, getId(ids, "sourceID"), getId(ids, "pid"));
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
}
