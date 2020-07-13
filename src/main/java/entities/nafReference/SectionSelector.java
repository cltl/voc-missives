package entities.nafReference;

import utils.common.Fragment;
import xjc.naf.Wf;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Selects section fragments for Conll output/input
 */
public class SectionSelector {

    static final Pattern TEXT = Pattern.compile(".*\\.p\\.\\d+$");
    static final Pattern FW = Pattern.compile(".*\\.fw\\.\\d+$");
    static final Pattern NOTE = Pattern.compile(".*\\.note\\.\\d+$");
    static final Pattern HEAD = Pattern.compile(".*\\.head\\.\\d+$");
    static final Pattern TABLE = Pattern.compile(".*\\.table\\.\\d+$");

    public static final Predicate<Fragment> TEXT_SELECTOR = x -> TEXT.matcher(x.getId()).matches()
            || HEAD.matcher(x.getId()).matches()
            || TABLE.matcher(x.getId()).matches() ;
    public static final Predicate<Fragment> NOTE_SELECTOR = x -> NOTE.matcher(x.getId()).matches();
    public static final Predicate<Fragment> ALL_SELECTOR = x -> true;
    public static final Predicate<Fragment> MIXED_SELECTOR = x -> ! FW.matcher(x.getId()).matches();

    Predicate<Fragment> selector;
    List<Fragment> selectedSections;

    public SectionSelector(String textType, List<Fragment> sections) {
        this.selector = getSelector(textType);
        this.selectedSections = select(sections);
    }

    private List<Fragment> select(List<Fragment> sections) {
        return Fragment.flatten(sections).stream().filter(s -> s.getLength() > 0).filter(selector).collect(Collectors.toList());
    }

    private Predicate<Fragment> getSelector(String textType) {
        if (textType.equals("text"))
            return TEXT_SELECTOR;
        else if (textType.equals("notes"))
            return NOTE_SELECTOR;
        else if (textType.equals("mixed"))
            return MIXED_SELECTOR;
        else if (textType.equals("all"))
            return ALL_SELECTOR;
        else
            throw new IllegalArgumentException("Unexpected text selector: " + textType + "\nAllowed types are: text|notes|mixed|all");
    }


    /**
     * Filters word forms that are part of <code>selectedSections</code>.
     * Assumes that both section and token lists are ordered.
     * @param wfs
     * @return
     */
    public List<Wf> filter(List<Wf> wfs) {
        List<Wf> filtered = new LinkedList<>();
        Iterator<Fragment> sectionIterator = selectedSections.iterator();
        if (sectionIterator.hasNext()) {
            Fragment currentSection = sectionIterator.next();
            for (Wf wf : wfs) {
                Fragment f = new Fragment(wf.getId(), wf.getOffset(), wf.getLength());
                if (currentSection.precedes(f)) {
                    if (sectionIterator.hasNext())
                        currentSection = sectionIterator.next();
                    else
                        break;
                }
                if (currentSection.contains(f))
                    filtered.add(wf);
            }
        }
        return filtered;
    }
}
