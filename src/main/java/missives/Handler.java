package missives;

import naf2tsv.Naf2Tsv;
import sysIn2naf.NAFConllReader;
import naf2conll.Naf2Conll;
import nafSelector.NafUnitSelector;
import org.apache.commons.cli.*;
import tei2naf.Tei2Naf;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import manIn2naf.NafXmiReader;

import java.util.Collections;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Main class for missives processing.
 *
 */
public class Handler {
    public final static String NAME = "gm-processor";
    public final static String VERSION = "1.0";

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "missives handler", options);
        System.exit(1);
    }

    public static void process(Options options, String[] args) throws AbnormalProcessException {

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (! cmd.hasOption('i')) {
                System.out.println("Please specify an input file or directory");
                usage(options);
            }
            final String indir = cmd.getOptionValue('i');
            String inputType = cmd.hasOption("I") ? cmd.getOptionValue('I') : IO.inferType(indir);
            if (! isKnownInputType(inputType))
                throw new IllegalArgumentException("input type " + inputType + " is invalid. Select one type among xml (tei), naf, entities.xmi or conll.");

            final String outdir = cmd.hasOption('o') ? cmd.getOptionValue('o') : "";
            String outputType = cmd.hasOption("O") ? cmd.getOptionValue('O') : IO.NAF_SFX;
            String conllSeparator = cmd.hasOption("c") ? cmd.getOptionValue('c') : " ";
            String documentType = cmd.hasOption("d") ? cmd.getOptionValue('d') : "tf";
            boolean tokenize = ! cmd.hasOption('n');
            if (cmd.hasOption('r') || cmd.hasOption('R')) {
                final String refdir = cmd.getOptionValue('r');
                String refType = cmd.hasOption("R") ? cmd.getOptionValue('R') : IO.inferType(refdir);
                if (! (refType.equals(IO.NAF_SFX)))
                    throw new IllegalArgumentException("Invalid reference type. Only 'naf' is allowed.");
                runConfiguration(indir, inputType, outdir, outputType, refdir, refType, cmd.hasOption('m'));
            } else
                runConfiguration(indir, inputType, outdir, outputType, tokenize, conllSeparator, documentType);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            usage(options);
        }
    }

    private static void runConfiguration(String indir, String inputType,
                                         String outdir, String outputType,
                                         String refDir, String refType,
                                         boolean manualConll) throws AbnormalProcessException {
        if (outputType.equals(IO.NAF_SFX) && refType.equals(IO.NAF_SFX)) {
            if (inputType.equals(IO.CONLL_SFX)) {
                if (manualConll)
                    IO.loop(indir, Collections.singletonList(refDir), outdir,
                            throwingBiConsumerWrapper((x, y) -> NafXmiReader.runWithConnl2Xmi(x, y)));
                else
                    IO.loop(indir, Collections.singletonList(refDir), outdir,
                            throwingBiConsumerWrapper((x, y) -> NAFConllReader.run(x, y)));
            } else if (inputType.equals(IO.XMI_SFX))
                IO.loop(indir, Collections.singletonList(refDir), outdir,
                        throwingBiConsumerWrapper((x, y) -> NafXmiReader.run(x, y)));
            else
                throw new IllegalArgumentException("Annotations entity-integration only supports Conll or XMI input");
        } else
            throw new IllegalArgumentException("Annotations entity-integration only supports NAF reference and output");
    }

    private static void runConfiguration(String indir,
                                         String inputType,
                                         String outdir,
                                         String outputType,
                                         boolean tokenize,
                                         String conllSeparator,
                                         String selectText) throws AbnormalProcessException {
        if (inputType.equals(IO.TEI_SFX) && outputType.equals(IO.NAF_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Tei2Naf.convertFile(x, y)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.NAF_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> NafUnitSelector.run(x, y, tokenize, selectText)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.CONLL_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Naf2Conll.run(x, y, conllSeparator)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.TSV_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Naf2Tsv.run(x, y)));
        else
            throw new IllegalArgumentException("conversion from " + inputType + " to " + outputType + " is not supported");
    }

    private static boolean isKnownInputType(String t) {
        return t.equals(IO.NAF_SFX) || t.equals(IO.TEI_SFX) || t.equals(IO.CONLL_SFX) || t.equals(IO.XMI_SFX);
    }

    public static void main(String[] args) throws AbnormalProcessException {
        Options options = new Options();
        options.addOption("i", true, "input file / directory");
        options.addOption("o", true, "output directory. Defaults to current directory");
        options.addOption("r", true, "reference directory for NE annotations entity-integration");
        options.addOption("I", true, "input file type (tei|naf|conll|xmi); inferred by default from input files extension: 'tei', 'naf', 'xmi' or 'conll'");
        options.addOption("O", true, "output file type (naf|conll); default: naf");
        options.addOption("R", true, "reference file type (naf); inferred by default from reference files extension ");
        options.addOption("d", true, "selected document type for reference NAF: text|notes|all|tf; default: tf");
        options.addOption("c", true, "conll separator for Conll output; defaults to single space");
        options.addOption("n", false, "do not tokenize reference NAF");
        options.addOption("m", false, "integrate manual Conll annotations");
        process(options, args);
    }
}
