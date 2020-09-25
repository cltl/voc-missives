package deprecated.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class Segments {

    List<Segment> segments;

    public Segments() {
        this.segments = new LinkedList<>();
    }

    private Segments(List<Segments> segmentsList) {
        this.segments = segmentsList.stream().map(s -> s.getSegments()).flatMap(x -> x.stream()).collect(Collectors.toList());
    }

    public void addSegment(int offset, int length, int index) {
        Segment segment = new Segment(offset, offset + length, index);
        segments.add(segment);
    }

    public int endIndex() {
        return segments.get(segments.size() - 1).getEnd();
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public static Segments join(List<Segments> segments) {
        return new Segments(segments);
    }
}
