package deprecated.rawTextAligner;

import org.junit.jupiter.api.Test;
import utils.naf.Fragment;
import utils.common.Span;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Deprecated
class OffsetMapperTest {

    @Test
    public void testSubFragments() {
        String text = "the horse raced past the barn fell.";
        Fragment f = new Fragment("x", 3, 18);
        assertEquals(text.substring(f.getOffset(), f.getEndIndex()), " horse raced past ");
        List<Span> fragments = OffsetMapper.subFragment(Collections.singletonList(f), text);
        assertEquals(fragments.size(), 3);

        assertEquals(fragments.get(0), new Span(4, 8));
        assertEquals(fragments.get(1), new Span(10, 14));
        assertEquals(fragments.get(2), new Span(16, 19));
    }

}