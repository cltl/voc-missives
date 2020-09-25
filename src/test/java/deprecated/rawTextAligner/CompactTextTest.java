package deprecated.rawTextAligner;

import org.junit.jupiter.api.Test;
import utils.common.Span;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Deprecated
class CompactTextTest {
    @Test
    public void testCompact() {
        String input = "1111 222 333";
        List<Span> sections = new LinkedList<>();
        sections.add(new Span(3,5));
        sections.add(new Span(7,11));
        CompactText ct = CompactText.create(input, sections);

        assertEquals(ct.getCompactText(), "1 22 333");
        assertEquals(ct.getRawCharOffset(0), 3);
        assertEquals(ct.getRawCharOffset(2), 5);
        assertEquals(ct.getRawCharOffset(3), 7);
        assertEquals(ct.getRawCharOffset(7), 11);
    }

}