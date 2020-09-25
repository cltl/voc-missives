package utils.common;

import org.junit.jupiter.api.Test;
import utils.naf.Fragment;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FragmentTest {

    @Test
    public void testFlattenAnyTreeDepth() {
        List<Fragment> fs = new LinkedList<>();
        fs.add(new Fragment("f1", 0, 21));
        fs.add(new Fragment("f21", 5, 5));
        fs.add(new Fragment("f22", 15, 5));
        fs.add(new Fragment("f31", 15, 4));
        fs.add(new Fragment("f32", 15, 0)); // exclude that one
        fs.add(new Fragment("f4", 16, 2));

        // keep all leaves and segmented non leaves
        List<Fragment> flatSegments = Fragment.flatten2(fs);

        assertEquals(flatSegments.size(), 8);
        String[] ids = new String[]{"f1", "f21", "f1", "f31", "f4", "f31", "f22", "f1"};
        for (int i = 0; i < flatSegments.size(); i++ ) {
            assertEquals(ids[i], flatSegments.get(i).getId());
        }
    }


}