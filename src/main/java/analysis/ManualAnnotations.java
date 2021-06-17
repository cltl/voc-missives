package analysis;

import utils.common.AbnormalProcessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * entity and token counts for XMI (and Conll) annotations
 */
public class ManualAnnotations {

    private AnnotationCounts textCounts;
    private AnnotationCounts notesCounts;
    private AnnotationCounts totalCounts;

    private ManualAnnotations(List<Path> paths) {
        this.textCounts = new AnnotationCounts(getPaths(paths, "text"));
        this.notesCounts = new AnnotationCounts(getPaths(paths, "notes"));
    }

    private List<Path> getPaths(List<Path> paths, String type) {
        return paths.stream().filter(p -> p.getFileName().toString().contains(type)).collect(Collectors.toList());
    }

    public static void collectStats(String indir) throws AbnormalProcessException {
        if (! Files.exists(Paths.get(indir)))
            throw new AbnormalProcessException("Directory " + indir + " does not exist.");
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            ManualAnnotations annotations = new ManualAnnotations(paths.filter(Files::isRegularFile).collect(Collectors.toList()));
            annotations.count();
            System.out.println(annotations.report());
        } catch (IOException e) {
            e.printStackTrace();
            throw new AbnormalProcessException("error looping through files in " + indir);
        }
    }

    private String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("---- Entity-count report ----\n---------------------------\nHistorical Texts:\n--------------\n")
                .append(textCounts.report())
                .append("\n\nEditorial Notes:\n--------------\n")
                .append(notesCounts.report())
                .append("\n\nTotal:\n--------------\n")
                .append(totalCounts.report())
                .append("\n\nMentions for rare types:\n--------------\n")
                .append(totalCounts.rareTypeMentions())
                .append("\n---------------------------");

        return sb.toString();
    }

    private void count() {
        textCounts.count();
        notesCounts.count();
        this.totalCounts = new AnnotationCounts(textCounts, notesCounts);
    }


}
