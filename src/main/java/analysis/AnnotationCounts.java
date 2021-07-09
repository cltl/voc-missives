package analysis;

import manIn2naf.Conll2Xmi;
import manIn2naf.IndexedEntity;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.NafHandler;
import utils.naf.NafUnits;
import utils.xmi.CasDoc;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationCounts {
    int tokenCount;
    int embedded;
    Map<String,Integer> entityTypes;
    Map<List,Integer> doubleAnnotations;
    List<IndexedEntity> entities;
    List<Path> paths;
    Map<String,List<String>> mentions;
    Map<String,List<String>> embeddedMentions;
    final static List<String> detailMentionTypes = new ArrayList<>(Arrays.asList(new String[]{"null", "SHPderiv", "LOCpart", "ORGpart", "OTH"}));


    protected AnnotationCounts(List<Path> paths) {
        this.entityTypes = new HashMap<>();
        this.doubleAnnotations = new HashMap<>();
        this.entities = new ArrayList<>();
        this.mentions = new HashMap<>();
        this.embeddedMentions = new HashMap<>();
        this.paths = paths;
    }

    public AnnotationCounts(AnnotationCounts textCounts, AnnotationCounts notesCounts) {
        this.embedded = textCounts.getEmbedded() + notesCounts.getEmbedded();
        this.tokenCount = textCounts.getTokenCount() + notesCounts.getTokenCount();
        this.doubleAnnotations = new HashMap<>();
        this.doubleAnnotations.putAll(textCounts.getDoubleAnnotations());
        update(this.doubleAnnotations, notesCounts.getDoubleAnnotations());
        this.entityTypes = new HashMap<>();
        this.entityTypes.putAll(textCounts.getEntityTypes());
        update(this.entityTypes, notesCounts.getEntityTypes());
        this.mentions = new HashMap<>();
        this.mentions.putAll(textCounts.getMentions());
        updateMentions(this.mentions, notesCounts.getMentions());
        this.embeddedMentions = new HashMap<>();
        this.embeddedMentions.putAll(textCounts.getEmbeddedMentions());
        updateMentions(this.embeddedMentions, notesCounts.getEmbeddedMentions());
    }

    public String report() {
        int entityCounts = entityTypes.values().stream().reduce(0, Integer::sum);
        int doubleCounts = doubleAnnotations.values().stream().reduce(0, Integer::sum);
        StringBuilder sb = new StringBuilder();
        sb.append("\ntoken and entity counts\n")
                .append(tokenCount).append(" tokens; ").append(entityCounts - doubleCounts).append(" entity labels; ")
                .append(doubleCounts).append(" double-label; ").append(embedded).append(" embedded")
                .append("\ndouble labels\n")
                .append(doubleAnnotations.entrySet().stream().map(e -> formatDouble(e)).collect(Collectors.joining(" ")))
                .append("\nentity types\n")
                .append(entityTypes.entrySet().stream().map(e -> format(e)).collect(Collectors.joining(" ")));
        return sb.toString();
    }

    private String format(Map.Entry<String,Integer> e) {
        return e.getKey() + ": " + e.getValue() + ";";
    }

    private String formatDouble(Map.Entry<List,Integer> e) {
        return e.getKey().stream().collect(Collectors.joining("_")) + ": " + e.getValue() + ";";
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public int getEmbedded() {
        return embedded;
    }

    public Map<String, List<String>> getEmbeddedMentions() {
        return embeddedMentions;
    }

    public Map<String, List<String>> getMentions() {
        return mentions;
    }

    public Map<String, Integer> getEntityTypes() {
        return entityTypes;
    }

    public Map<List, Integer> getDoubleAnnotations() {
        return doubleAnnotations;
    }

    private void extractTokenCountsAndEntities(List<Path> paths) {
        paths.forEach(p -> {
            try {
                extractTokensAndEntities(p);
            } catch (AbnormalProcessException e) {
                e.printStackTrace();
            }
        });
    }

    private void extractTokensAndEntities(Path p) throws AbnormalProcessException {
        String extension = IO.extension(p.getFileName().toString());
        if (extension.equals("conll")) {
            Conll2Xmi conll2Xmi = new Conll2Xmi(p.toString());
            conll2Xmi.convert();
            getTokensAndEntities(conll2Xmi);
        } else if (extension.equals("xmi"))
            getTokensAndEntities(CasDoc.create(p.toString()));
        else if (extension.equals("naf"))
            getTokensAndEntities(NafHandler.create(p.toString()));
    }

    private void getTokensAndEntities(NafHandler naf) {
        this.tokenCount += naf.getWfs().size();
        this.entities.addAll(naf.getEntities().stream()
                .map(e -> IndexedEntity.create(naf.coveredText(e), e.getType(), NafUnits.indexSpan(e)))
                .collect(Collectors.toList()));
    }
    private void getTokensAndEntities(Conll2Xmi conll2Xmi) {
        this.tokenCount += conll2Xmi.getTokens().size();
        this.entities.addAll(conll2Xmi.getXmi().getEntities().stream()
                .map(e -> IndexedEntity.create(e.getCoveredText(), e.getValue(), e.getBegin(), e.getEnd() + 1))
                .collect(Collectors.toList()));
    }
    private void getTokensAndEntities(CasDoc xmi) {
        this.tokenCount += xmi.getTokens().size();
        this.entities.addAll(xmi.getEntities().stream()
                .map(e -> IndexedEntity.create(e.getCoveredText(), e.getValue(), e.getBegin(), e.getEnd() + 1))
                .collect(Collectors.toList()));
    }

    protected void count() {
        extractTokenCountsAndEntities(paths);
        detailedCounts();
    }

    private void detailedCounts() {
        if (! entities.isEmpty()) {
            IndexedEntity previous = entities.get(0);
            increment(entityTypes, previous.getType());
            addToRareMentions(previous);
            for (int i = 1; i < entities.size(); i++) {
                if (entities.get(i).hasSameSpan(previous)) {
                    ArrayList<String> labels = new ArrayList<>(Arrays.asList(new String[]{entities.get(i).getType(), previous.getType()}));
                    Collections.sort(labels);
                    increment(doubleAnnotations, labels);
                    increment(entityTypes, entities.get(i).getType());
                } else if (entities.get(i).isEmbeddedIn(previous)) {
                    embedded += 1;
                    addToEmbeddedMentions(previous, entities.get(i));
                    increment(entityTypes, entities.get(i).getType());
                } else {
                    increment(entityTypes, entities.get(i).getType());
                }
                addToRareMentions(entities.get(i));
                previous = entities.get(i);
            }
        }
    }

    private void addToEmbeddedMentions(IndexedEntity entity, IndexedEntity embedded) {
        addTo(embeddedMentions, entity.getType() + "-" + embedded.getType(), entity.getToken());
    }

    private void addToRareMentions(IndexedEntity e) {
        if (e.getType() == null)
            addTo(mentions, "null", e.getToken());
        else if (detailMentionTypes.contains(e.getType())) {
            addTo(mentions, e.getType(), e.getToken());
        }
    }

    private void addTo(Map<String,List<String>> map, String type, String mention) {
        if (map.containsKey(type)) {
            List<String> v = map.get(type);
            v.add(mention);
            map.put(type, v);
        } else {
            List<String> v = new LinkedList<>();
            v.add(mention);
            map.put(type, v);
        }
    }

    private void updateMentions(Map<String,List<String>> map1, Map<String,List<String>> map2) {
        for (String key: map2.keySet()) {
            if (map1.containsKey(key)) {
                List<String> value = map1.get(key);
                value.addAll(map2.get(key));
                map1.put(key, value);
            } else
                map1.put(key, map2.get(key));
        }
    }

    private <T> void update(Map<T, Integer> map1, Map<T, Integer> map2) {
        for (T key: map2.keySet()) {
            if (map1.containsKey(key))
                map1.put(key, map1.get(key) + map2.get(key));
            else
                map1.put(key, map2.get(key));
        }
    }

    private <T> void increment(Map<T, Integer> map, T k) {
        if (map.containsKey(k))
            map.put(k, map.get(k) + 1);
        else
            map.put(k, 1);
    }

    public String embeddedMentions() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry: embeddedMentions.entrySet()) {
            sb.append(" -- ").append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" -- \n");
            entry.getValue().forEach(v -> sb.append(v).append("\n"));
        }
        return sb.toString();
    }

    public String rareTypeMentions() {
        StringBuilder sb = new StringBuilder();
        for (String type: detailMentionTypes) {
            if (mentions.containsKey(type)) {
                sb.append(" -- ").append(type).append(" -- \n");
                mentions.get(type).forEach(m -> sb.append(m).append("\n"));
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
