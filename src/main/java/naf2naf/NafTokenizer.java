package naf2naf;

import missives.Handler;
import utils.common.AbnormalProcessException;
import utils.common.Pair;
import utils.common.Span;
import utils.naf.NafHandler;
import utils.naf.Wf;
import xjc.naf.Tunit;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class NafTokenizer {
    NafHandler naf;
    private final static String NAME = "ixa-pipe-tok";

    public NafTokenizer(String nafFile) throws AbnormalProcessException {
        this.naf = NafHandler.create(nafFile);
    }

    protected void writeNaf(String outdir) throws AbnormalProcessException {
        naf.writeToDir(outdir);
    }

    public NafHandler getNaf() {
        return naf;
    }

    void tokenize() throws AbnormalProcessException {
        List<Tunit> nafUnits = naf.getTunits();
        nafUnits = nafUnits.stream().filter(t -> ! t.getLength().equals("0")).collect(Collectors.toList());
        if (isTreeLike(nafUnits))
            nafUnits = flatten(nafUnits);
        Tokenizer tokenizer = Tokenizer.create();
        List<Wf> wfs = tokenizer.getWfs(getUnitsOffsetAndText(nafUnits));
        naf.createTextLayer(wfs, getName(), tokenizer.version());
    }

    private List<Pair<Integer, String>> getUnitsOffsetAndText(List<Tunit> tunits) {
        return tunits.stream().map(tunit ->
                new Pair<>(Integer.parseInt(tunit.getOffset()),
                        naf.coveredText(tunit.getOffset(), tunit.getLength())))
                .collect(Collectors.toList());
    }
    private boolean isTreeLike(List<Tunit> nafUnits) {
        for (int i = 0; i < nafUnits.size() - 1; i++) {
            if (contains(nafUnits.get(i), nafUnits.get(i + 1)))
                return true;
        }
        return false;
    }

    private boolean contains(Tunit tunit1, Tunit tunit2) {
        return Span.fromCharPosition(tunit1.getOffset(), tunit1.getLength())
                .contains(Span.fromCharPosition(tunit2.getOffset(), tunit2.getLength()));
    }

    private List<Tunit> flatten(List<Tunit> nafUnits) {
        TunitTree tunitTree = TunitTree.create(nafUnits);
        return tunitTree.extractTunits(x -> false);
    }

    public static void run(Path file, String outdir) throws AbnormalProcessException {
        NafTokenizer nafTokenizer = new NafTokenizer(file.toString());
        nafTokenizer.tokenize();
        nafTokenizer.writeNaf(outdir);
    }

    public String getName() {
        return Handler.NAME + "-" + NAME;
    }

}
