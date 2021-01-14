package missives;

import conllin2naf.NAFConllReader;
import naf2conll.Naf2Conll;
import nafSelector.NafUnitSelector;
import org.apache.commons.cli.*;
import tei2naf.Tei2Naf;
import utils.common.IO;
import xmiIn2naf.NafXmiReader;

import java.util.Collections;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;

/**
 * Main class for missives processing.
 *
 */
public class Handler {


    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "missives handler", options);
        System.exit(1);
    }

    public static void process(Options options, String[] args) {

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (! cmd.hasOption('i')) {
                System.out.println("Please specify an input file or directory");
                usage(options);
            }
            final String indir = cmd.getOptionValue('i');
            String inputType = cmd.hasOption("I") ? cmd.getOptionValue('I') : IO.inferType(indir);
            if (! isKnownType(inputType))
                throw new IllegalArgumentException("input type " + inputType + " is invalid. Select one type among xml (tei), naf, entities.xmi or conll.");

            final String outdir = cmd.hasOption('o') ? cmd.getOptionValue('o') : "";
            String outputType = cmd.hasOption("O") ? cmd.getOptionValue('O') : IO.NAF_SFX;
            String conllSeparator = cmd.hasOption("c") ? cmd.getOptionValue('c') : " ";
            String documentType = cmd.hasOption("d") ? cmd.getOptionValue('d') : "all";
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
                                         boolean manualConll) {
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
                throw new IllegalArgumentException("Annotations integration only supports Conll or XMI input");
        } else
            throw new IllegalArgumentException("Annotations integration only supports NAF reference and output");
    }

    private static void runConfiguration(String indir,
                                         String inputType,
                                         String outdir,
                                         String outputType,
                                         boolean tokenize,
                                         String conllSeparator,
                                         String selectText) {
        if (inputType.equals(IO.TEI_SFX) && outputType.equals(IO.NAF_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Tei2Naf.convertFile(x, y)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.NAF_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> NafUnitSelector.run(x, y, tokenize, selectText)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.CONLL_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Naf2Conll.run(x, y, conllSeparator)));
        else
            throw new IllegalArgumentException("conversion from " + inputType + " to " + outputType + " is not supported");
    }

    private static boolean isKnownType(String t) {
        return t.equals(IO.NAF_SFX) || t.equals(IO.TEI_SFX) || t.equals(IO.CONLL_SFX) || t.equals(IO.XMI_SFX);
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", true, "input file / directory");
        options.addOption("o", true, "output directory. Defaults to current directory");
        options.addOption("r", true, "reference directory for NE annotations integration");
        options.addOption("I", true, "input file type (xml|naf|conll|xmi); inferred by default from input files extension: 'tei', 'naf', 'xmi' or 'conll'");
        options.addOption("O", true, "output file type; default: NAF");
        options.addOption("R", true, "reference file type (naf|xmi); inferred by default from reference files extension ");
        options.addOption("d", true, "selected document type for reference NAF: text|notes|all; default:all");
        options.addOption("c", true, "conll separator for Conll output; defaults to single space");
        options.addOption("n", false, "do not tokenize reference NAF");
        options.addOption("m", false, "integrate manual Conll annotations");
        process(options, args);
    }
}
