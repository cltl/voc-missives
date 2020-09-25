package deprecated.nafReference;

import javafx.util.Pair;
import utils.naf.Fragment;
import utils.common.*;
import deprecated.utils.BaseEntity;
import deprecated.utils.BaseToken;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Aligns input to reference tokens by string matching
 * The input is read from Conll files.
 *
 * Token alignment aims at finding a matching reference token for each input token.
 * This is trivial if the Conll files were produced from the NAF, but more difficult
 * if tokenization differs, and even more if the text differs. In the latter case,
 * one must not only consider many-to-many alignments but also null alignments.
 *
 * Connl-to-Naf token aligment may take the following forms:
 *
 * - one-to-one         -- the ideal case
 * - one-to-many        -- one conll token coarseMatches a NAF token span
 * - many-to-one        -- several conll tokens point to a same NAF token
 * - many-to-many       -- substrings match over a n-to-m sequence
 * - one/many-to-null   -- conll tokens absent from NAF
 * - null-to-one/many   -- NAF tokens missing from Conll
 *
 * Objects
 *
 * - last aligned token span (conll span and naf span), or last aligned indices
 * - aligned span for conll entities
 */
@Deprecated
public class ConllAligner {
    String conllFile;
    /**
     * derived from Naf Wfs
     */
    List<BaseToken> refTokens;
    String sep;
    /**
     * maps conll token index to <code>refTokens</code> index
     */
    List<Integer> refIds;
    List<BaseEntity> entities;

    ArrayDeque<Pair<Span,Span>> alignedSpans;
    ArrayDeque<Pair<Span,String>> conllEntities;
    int iRef;
    int iConll;
    String alignedString;
    Pair<Fragment,Fragment> alignedFragment;

    public ConllAligner(String conllFile, List<BaseToken> refTokens, String connlSep) {
        this.conllFile = conllFile;
        this.refTokens = refTokens;
        this.refIds = new ArrayList<>();
        this.entities = new LinkedList<>();
        this.sep = connlSep;
        this.iRef = 0;
        this.alignedSpans = new ArrayDeque<>();
        this.conllEntities = new ArrayDeque<>();
        this.alignedString = "";
        this.alignedFragment = null;
    }

    private void align(String conll, int first, int last) {
        boolean nbAlignedTargets = alignOneToOneOrMany(conll);

    }

    private boolean alignOneToOneOrMany(String conll) {
        String refCat = refTokens.get(iRef).getText();
        int k = 0;
        while (conll.startsWith(refCat)) {
            if (conll.equals(refCat)) {
                alignedSpans.add(new Pair<>(new Span(iConll, iConll), new Span(iRef, iRef + k)));
                iRef += k + 1;
                iConll++;
                return true;
            }
            k++;
            refCat += refTokens.get(iRef + k);
        }
        return false;
    }

    private boolean alignManyToOne(String conll) {
        if (refTokens.get(iRef).getText().startsWith(conll)) {

        }
        return false;
    }

    private void queueLabel(String label) {
        if (label.startsWith("B")) {
            conllEntities.add(new Pair<>(new Span(iConll, iConll), label.substring(2)));
        } else if (label.startsWith("I")) {
            conllEntities.peekLast().getKey().setLastIndex(iConll);
        }
    }

    private void queueToken(String conll) {

    }

    private void read() throws AbnormalProcessException {
        String line = null;
        int firstEntityTokenIndex = -1;
        int lastEntityTokenIndex = -1;
        try (BufferedReader bfr = new BufferedReader(new FileReader(conllFile))) {
            while ((line = bfr.readLine()) != null) {
                String[] tokenLabel = line.split(sep);
                queueLabel(tokenLabel[1]);
                if (! alignOneToOneOrMany(tokenLabel[0])) {
                    queueToken(tokenLabel[0]);
                }
            }
        } catch (FileNotFoundException e) {
            throw new AbnormalProcessException(conllFile, e);
        } catch (IOException e) {
            throw new AbnormalProcessException(conllFile, e);
        }
    }
}
