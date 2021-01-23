package manIn2naf;

import utils.common.Span;
import xjc.naf.Entity;

import java.util.LinkedList;
import java.util.List;

public interface EntityAligner {

    static List<Span> match(String surfaceForm, String rawText) {
        List<Span> matches = new LinkedList<>();
        int index = rawText.indexOf(surfaceForm);
        while (index != -1) {
            matches.add(new Span(index, index + surfaceForm.length() - 1));
            index = rawText.indexOf(surfaceForm, index + 1);
        }
        return matches;
    }

    List<Entity> align();

    void logStats();
}
