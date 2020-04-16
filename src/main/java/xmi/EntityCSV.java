package xmi;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import tei2xmi.Converter;
import utils.*;
import xjc.tei.TEI;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static utils.ThrowingBiConsumer.throwingBiConsumerWrapper;

public class EntityCSV {

    /**
     * Maps reporting locations to anchored entities (map of text/note to type to entity to counts)
     */
    AnchoredEntities anchoredEntities;

    /**
     * maps page numbers to tei ids
     */
    HashMap<String, List<String>> pagesToTeiIDs;
    /**
     * lists tei ids in order
     */
    List<String> orderedIds;

    /**
     * maps tei id of paragraph matching an entity location to location string
     */
    HashMap<String,String> idsToLocations;
    String volume;
    String date;
    String defaultLocation;

    public EntityCSV() {
        this.pagesToTeiIDs = new HashMap<>();
        this.orderedIds = new LinkedList<>();
        this.idsToLocations = new HashMap<>();
        this.anchoredEntities = new AnchoredEntities();
        this.defaultLocation = "Unknown";
    }

    static EntityCSV create(String teiFile) throws AbnormalProcessException {
        EntityCSV entityCSV = new EntityCSV();
        entityCSV.getOrderedIds(teiFile);
        return entityCSV;
    }

    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".xmi")) {
            String pfx = fileName.replaceAll("\\.xmi", "");
            String noteFile = IO.append(dirs.get(0), pfx + "_notes.xmi");
            String teiFile = IO.append(dirs.get(1), pfx + ".xml");
            String outFile = IO.append(dirs.get(2), pfx + ".csv");

            EntityCSV entityCSV = EntityCSV.create(teiFile);
            entityCSV.segmentPerLocation(file.toString());
            if (existNotes(noteFile)) {
                entityCSV.segmentNotesPerLocation(noteFile);
            }
            entityCSV.write(outFile);
        }
    }

    private void segmentNotesPerLocation(String noteFile) throws AbnormalProcessException {
        CasDoc doc = CasDoc.create();
        doc.read(noteFile);
        List<NamedEntity> entities = doc.getEntities();
        List<Paragraph> paragraphs = doc.getParagraphs();
        HashMap<String,String> noteIdsToLocations = new HashMap<>();
        paragraphs.forEach(p -> noteIdsToLocations.put(p.getId(), getReportingLocation(p.getId())));
        anchoredEntities.initNoteMap();
        for (NamedEntity e: entities) {
            Paragraph paragraph = getEnclosingParagraph(paragraphs, e);
            String location = noteIdsToLocations.get(paragraph.getId());
            anchoredEntities.updateNoteInstance(location, e.getValue(), e.getCoveredText());
        }
    }

    private Paragraph getEnclosingParagraph(List<Paragraph> paragraphs, NamedEntity e) {
        List<Paragraph> paragraphList = paragraphs.stream().filter(p -> p.getBegin() <= e.getBegin() && e.getEnd() <= p.getEnd()).collect(Collectors.toList());
        if (paragraphList.isEmpty()) // some entities cross paragraph boundaries
            paragraphList = paragraphs.stream().filter(p -> p.getBegin() <= e.getBegin() && e.getBegin() <= p.getEnd()).collect(Collectors.toList());
        return paragraphList.get(0);
    }

    /**
     * find first location id preceding this id and return location string.
     * @param id
     * @return
     */
    private String getReportingLocation(String id) {
        for (int i = orderedIds.indexOf(id); i >= 0; i--) {
            if (idsToLocations.containsKey(orderedIds.get(i)))
                return idsToLocations.get(orderedIds.get(i));
        }
        return "Batavia";
    }

    /**
     * identifies entities corresponding to location headers
     * @param file
     */
    private void segmentPerLocation(String file) throws AbnormalProcessException {
        CasDoc doc = CasDoc.create();
        doc.read(file);
        this.idsToLocations = addTextEntities(doc);
        this.volume = doc.getVolumeId();
        this.date = doc.getDate();

    }

    private HashMap<String, String> addTextEntities(CasDoc doc) {
        List<NamedEntity> entities = doc.getEntities();
        List<Paragraph> paragraphs = doc.getParagraphs();
        HashMap<String,String> idsToLocations = new HashMap<>();
        if (! matchSpans(paragraphs.get(0), entities.get(0))) {
            if (paragraphs.get(0).getCoveredText().toLowerCase().contains("batavia"))
                this.defaultLocation = "Batavia";
            idsToLocations.put(paragraphs.get(0).getId(), defaultLocation);
        }
        String currentLocation = this.defaultLocation;
        for (NamedEntity e: entities) {
            Paragraph paragraph = getEnclosingParagraph(paragraphs, e);
            if (matchSpans(paragraph, e) && e.getValue().equals("LOC")) {
                currentLocation = e.getCoveredText();
                idsToLocations.put(paragraph.getId(), currentLocation);
            }
            anchoredEntities.updateTextInstance(currentLocation, e.getValue(), e.getCoveredText());
        }
        return idsToLocations;
    }


    private boolean matchSpans(Paragraph paragraph, NamedEntity namedEntity) {
        return paragraph.getBegin() == namedEntity.getBegin() && paragraph.getEnd() == namedEntity.getEnd();
    }


    private void getOrderedIds(String file) throws AbnormalProcessException {
        TEI tei = Converter.load(file);
        ATeiTree tree;
        try {
            tree = TeiTreeFactory.create(tei);
        } catch (IllegalArgumentException e) {
            throw new AbnormalProcessException("Error while creating TEI tree", e);
        }
        String page = "";
        for (ATeiTree t: tree.getAllNodes(x -> true)) {
            if (t.getTeiType() == ATeiTree.TeiType.PB) {
                page = ((TeiBreak) t).getPageNumber();
                pagesToTeiIDs.put(page, new LinkedList<>());
            } else if (! page.equals("") && t.getId() != null){ // top elements (tei, body, etc.) come before the first page break
                pagesToTeiIDs.get(page).add(t.getId());
                orderedIds.add(t.getId());
            }
        }
    }

    private static boolean existNotes(String noteFile) {
        return Files.exists(Paths.get(noteFile));
    }

    private void write(String outFile) throws AbnormalProcessException {
        // TODO adapt to print: volume, date, textOrNote, location, page (of location header) or link, type, entity, nbMentions
        // TODO identify location headers
        // TODO may have to add page for link

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            HashMap<String, String> locationLinks = mapLocationLinks();
            for (AnchoredEntity a : anchoredEntities.values()) {
                StringBuilder sb = new StringBuilder();
                sb.append(volume).append(",")
                        .append(date).append(",")
                        .append(a.toString()).append(",")
                        .append(locationLinks.get(a.getLocation())).append("\n");
                bw.write(sb.toString());
            }
        } catch (IOException e) {
            throw new AbnormalProcessException("Error writing to " + outFile);
        } catch (NullPointerException e) {
            throw new AbnormalProcessException("Missing location link", e);
        }

    }

    private HashMap<String, String> mapLocationLinks() throws NullPointerException {
        HashMap<String,String> locationLinks = new HashMap<>();
        for (HashMap.Entry<String,String> e: idsToLocations.entrySet()) {
            String pageNb = getPageNumber(e.getKey());
            if (pageNb == null)
                throw new NullPointerException("empty page number for location: " + e.getValue());
            // TODO replace pagenb by url
            locationLinks.put(e.getValue(), pageNb);
        }
        return locationLinks;
    }

    private String getPageNumber(String id) {
        for (HashMap.Entry<String,List<String>> e: pagesToTeiIDs.entrySet()) {
            if (e.getValue().contains(id))
                return e.getKey();
        }
        System.out.println(id);
        return null;
    }


    /**
     * @param args input-text-dir, input-notes-dir, tei-dir, output-dir
     **/
    public static void main(String[] args) {
        String inputTextDir = args[0];
        String inputNoteDir = args[1];
        String teiDir = args[2];
        String outDir = args[3];
        List<String> auxdirs = new LinkedList<>();
        auxdirs.add(inputNoteDir);
        auxdirs.add(teiDir);
        IO.loop(args[0], auxdirs, args[3], throwingBiConsumerWrapper((x, y) -> run(x, y)));
    }
}
