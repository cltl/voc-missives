package analysis;

import manIn2naf.IndexedEntity;
import utils.common.AbnormalProcessException;
import utils.xmi.CasDoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Agreement {
    List<IndexedEntity> shared;
    List<IndexedEntity> diffLabel1;
    List<IndexedEntity> diffLabel2;
    List<IndexedEntity> diffSpan1;
    List<IndexedEntity> diffSpan2;
    List<IndexedEntity> unaligned1;
    List<IndexedEntity> unaligned2;

    private Agreement(String path1, String path2) throws AbnormalProcessException {
        List<IndexedEntity> entities1 = loadEntities(path1);
        List<IndexedEntity> entities2 = loadEntities(path2);
        match(entities1, entities2);
    }

    protected Agreement(List<IndexedEntity> entities1, List<IndexedEntity> entities2) {
        match(entities1, entities2);
    }

    private Agreement(List<Agreement> fileAgreements) {
        this.shared = collectAll(fileAgreements, Agreement::getShared);
        this.diffLabel1 = collectAll(fileAgreements, Agreement::getDiffLabel1);
        this.diffLabel2 = collectAll(fileAgreements, Agreement::getDiffLabel2);
        this.diffSpan1 = collectAll(fileAgreements, Agreement::getDiffSpan1);
        this.diffSpan2 = collectAll(fileAgreements, Agreement::getDiffSpan2);
        this.unaligned1 = collectAll(fileAgreements, Agreement::getUnaligned1);
        this.unaligned2 = collectAll(fileAgreements, Agreement::getUnaligned2);
    }

    private List<IndexedEntity> collectAll(List<Agreement> files, Function<Agreement,List<IndexedEntity>> get) {
        return files.stream().map(get).flatMap(x -> x.stream()).collect(Collectors.toList());
    }

    public List<IndexedEntity> getShared() {
        return shared;
    }

    public List<IndexedEntity> getDiffLabel1() {
        return diffLabel1;
    }

    public List<IndexedEntity> getDiffLabel2() {
        return diffLabel2;
    }

    public List<IndexedEntity> getDiffSpan1() {
        return diffSpan1;
    }

    public List<IndexedEntity> getDiffSpan2() {
        return diffSpan2;
    }

    public List<IndexedEntity> getUnaligned1() {
        return unaligned1;
    }

    public List<IndexedEntity> getUnaligned2() {
        return unaligned2;
    }

    private void match(List<IndexedEntity> entities1, List<IndexedEntity> entities2) {
        this.shared = entities1.stream().filter(e -> entities2.contains(e)).collect(Collectors.toList());
        List<IndexedEntity> remaining1 = entities1.stream().filter(e -> ! shared.contains(e)).collect(Collectors.toList());
        List<IndexedEntity> remaining2 = entities2.stream().filter(e -> ! shared.contains(e)).collect(Collectors.toList());
        this.diffLabel2 = new ArrayList<>();
        this.diffLabel1 = new ArrayList<>();
        for (IndexedEntity e: remaining1) {
            List<IndexedEntity> matching2 = remaining2.stream().filter(e2 -> e2.hasSameSpan(e)).collect(Collectors.toList());
            if (matching2.size() == 1) {
                diffLabel1.add(e);
                diffLabel2.add(matching2.get(0));
            }
        }
        remaining1 = remaining1.stream().filter(e -> ! diffLabel1.contains(e)).collect(Collectors.toList());
        remaining2 = remaining2.stream().filter(e -> ! diffLabel2.contains(e)).collect(Collectors.toList());
        this.diffSpan2 = new ArrayList<>();
        this.diffSpan1 = new ArrayList<>();
        for (IndexedEntity e: remaining1) {
            List<IndexedEntity> matching2 = remaining2.stream().filter(e2 -> e2.overlapsWith(e)).collect(Collectors.toList());
            if (! matching2.isEmpty()) {
                diffSpan1.add(e);
                diffSpan2.add(matching2.get(0)); // only align the first match (others will count as unaligned)
            }
        }

        this.unaligned1 = remaining1.stream().filter(e -> ! diffSpan1.contains(e)).collect(Collectors.toList());
        this.unaligned2 = remaining2.stream().filter(e -> ! diffSpan2.contains(e)).collect(Collectors.toList());
    }

    private List<IndexedEntity> loadEntities(String path1) throws AbnormalProcessException {
        return CasDoc.create(path1).getEntities().stream()
                .map(e -> IndexedEntity.create(e.getCoveredText(), e.getValue(), e.getBegin(), e.getEnd()))
                .collect(Collectors.toList());
    }

    private double precision() {
        return shared.size() * 1.0 / counts2();
    }

    private double recall() {
        return shared.size() * 1.0 / counts1();
    }

    private double fscore() {
        return 2 * precision() * recall() / (precision() + recall());
    }

    private int counts2() {
        return shared.size() + diffLabel2.size() + diffSpan2.size() + unaligned2.size();
    }

    private int counts1() {
        return shared.size() + diffLabel1.size() + diffSpan1.size() + unaligned1.size();
    }

    public static void agreement(String indir) throws AbnormalProcessException {
        List<Path> annotators;
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            annotators = paths.filter(Files::isDirectory).filter(f -> ! f.toString().equals(Paths.get(indir).toString())).collect(Collectors.toList());
            if (annotators.size() != 2)
                throw new AbnormalProcessException("Expected to find 2 annotator directories, but found "
                        + annotators.size() + ":\n"
                        + annotators.stream().map(a -> a.toString()).collect(Collectors.joining("\n")));
        } catch (IOException e) {
            throw new AbnormalProcessException(e);
        }
        try (Stream<Path> annotationPaths = Files.walk(annotators.get(0))) {
                List<Agreement> fileAgreements = new LinkedList<>();
                for (Path p: annotationPaths.filter(Files::isRegularFile).collect(Collectors.toList()))
                    fileAgreements.add(Agreement.create(p.toString(),
                                Paths.get(annotators.get(1).toString(), p.getFileName().toString()).toString()));

                fileAgreements.forEach(Agreement::report);
                Agreement total = Agreement.cumulate(fileAgreements);
                total.fullReport();
        } catch (IOException e) {
            throw new AbnormalProcessException(e);
        }
    }

    public static Agreement create(String file1, String file2) throws AbnormalProcessException {
        return new Agreement(file1, file2);
    }

    public static Agreement cumulate(List<Agreement> agreements) {
        return new Agreement(agreements);
    }

    private List<Map.Entry<String, Integer>> diffLabelCounts() {
        Map<String,Integer> doubleLabels = new HashMap<>();
        for (int i = 0; i < diffLabel1.size(); i++) {
            List<String> labels = Arrays.asList(new String[]{diffLabel1.get(i).getType(), diffLabel2.get(i).getType()});
            Collections.sort(labels);
            String label = labels.stream().collect(Collectors.joining("-"));
            if (doubleLabels.containsKey(label))
                doubleLabels.put(label, doubleLabels.get(label) + 1);
            else
                doubleLabels.put(label, 1);
        }
        return doubleLabels.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());

    }

    /**
     * annotation pairs with a different label or span are assumed to stand for a single entity:
     * only unaligned entities differ per annotation set
     * @return the total count of annotated entities across the label set, counting shared entities only once
     */
    protected int entityCounts() {
        return counts1() + unaligned2.size();
    }

    protected double proportion(List<IndexedEntity> entities) {
        return entities.size() * 100.0 / entityCounts();
    }

    protected void report() {
        fscoreReport();
        pairingReport();
    }

    protected void fullReport() {
        report();
        detailedReport();
    }

    private void detailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Label disagreement ---\n\n-- label counts\n")
                .append(diffLabelCounts().stream().map(e -> e.getKey() + "\t" + e.getValue()).collect(Collectors.joining("\n")))
                .append("\n\n-- instances\n")
                .append(diffLabelInstances())
                .append("\n\n--- Span disagreement ---\n")
                .append(diffSpanInstances())
                .append("\n\n--- Entity disagreement ---\n-- set 1\n")
                .append(instances(unaligned1))
                .append("\n\n-- set 2\n")
                .append(instances(unaligned2));
        System.out.println(sb.toString());
    }

    private static String instances(List<IndexedEntity> entities) {
        return entities.stream().map(IndexedEntity::getLabelledMention).collect(Collectors.joining("; "));
    }

    private String diffLabelInstances() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < diffLabel1.size(); i++)
            sb.append(diffLabel1.get(i).getType()).append("-")
                    .append(diffLabel2.get(i).getType()).append(": ")
                    .append(diffLabel1.get(i).getToken()).append("\n");
        return sb.toString();
    }

    private String diffSpanInstances() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < diffSpan1.size(); i++)
            sb.append(diffSpan1.get(i).getLabelledMention()).append(" / ")
                    .append(diffSpan2.get(i).getLabelledMention()).append("\n");
        return sb.toString();
    }

    private void fscoreReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- F score ---\nprecision: ").append(precision()).append("\ttp: ").append(shared.size()).append("; ref").append(counts1())
                .append("\nrecall: ").append(recall()).append("\ttp: ").append(shared.size()).append("; ref").append(counts2())
                .append("\nF score: ").append(fscore())
                .append("\n------------------\n");
        System.out.println(sb.toString());
    }

    private List<IndexedEntity> unalignedEntities() {
        List<IndexedEntity> list = new ArrayList<>();
        list.addAll(unaligned1);
        list.addAll(unaligned2);
        return list;
    }

    private void pairingReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Annotation Pairing ---\nagreeing:\t").append(2 * shared.size())
                .append(" (").append(shared.size()).append("/").append(shared.size()).append(")\t")
                .append(proportion(shared))
                .append("\ndisagreeing:\n - label:\t").append(diffLabel1.size() + diffLabel2.size())
                .append( " (").append(diffLabel1.size()).append("/").append(diffLabel2.size()).append(")\t")
                .append(proportion(diffLabel1))
                .append("\n - span:\t").append(diffSpan1.size() + diffSpan2.size())
                .append( " (").append(diffSpan1.size()).append("/").append(diffSpan2.size()).append(")\t")
                .append(proportion(diffSpan1))
                .append("\n - entity:\t").append(unaligned1.size() + unaligned2.size())
                .append( " (").append(unaligned1.size()).append("/").append(unaligned2.size()).append(")\t")
                .append(proportion(unalignedEntities()))
                .append("\n------------------\n");
        System.out.println(sb.toString());
    }

}
