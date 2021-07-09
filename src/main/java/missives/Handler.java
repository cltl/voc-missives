package missives;

import analysis.Agreement;
import analysis.ManualAnnotations;
import naf2tsv.Naf2Tsv;
import naf2naf.NafTokenizer;
import conllIn2naf.NAFConllReader;
import naf2conll.Naf2Conll;
import naf2naf.NafUnitSelector;
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
    public final static String NAME = "voc-missives";
    public final static String VERSION = "1.1";

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

            if (cmd.hasOption('a'))
                analyse(cmd.getOptionValue('a'), cmd.getOptionValue('i'));
            else {
                final String indir = cmd.getOptionValue('i');
                String inputType = cmd.hasOption("I") ? cmd.getOptionValue('I') : IO.inferType(indir);
                if (!isKnownInputType(inputType))
                    throw new IllegalArgumentException("input type " + inputType + " is invalid. Select one type among xml (tei), naf, entities.xmi or conll.");

                final String outdir = cmd.hasOption('o') ? cmd.getOptionValue('o') : "";
                String outputType = cmd.hasOption("O") ? cmd.getOptionValue('O') : IO.NAF_SFX;
                String documentType = cmd.hasOption("d") ? cmd.getOptionValue('d') : "all";
                boolean tokenize = cmd.hasOption('t');
                if (cmd.hasOption('r') || cmd.hasOption('R')) {
                    final String refdir = cmd.getOptionValue('r');
                    String refType = cmd.hasOption("R") ? cmd.getOptionValue('R') : IO.inferType(refdir);
                    String entitySource = cmd.hasOption("e") ? cmd.getOptionValue('e') : "man";
                    String dataVersion = cmd.hasOption("v") ? cmd.getOptionValue('v') : "0";
                    if (!(refType.equals(IO.NAF_SFX)))
                        throw new IllegalArgumentException("Invalid reference type. Only 'naf' is allowed.");
                    runConfiguration(indir, inputType, outdir, outputType, refdir, refType, entitySource, dataVersion, cmd.hasOption('w'), cmd.hasOption('n'));
                } else
                    runConfiguration(indir, inputType, outdir, outputType, tokenize, documentType, cmd.hasOption('f'), cmd.hasOption('u'));
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            usage(options);
        }
    }

    private static void analyse(String analysis, String inputdir) throws AbnormalProcessException {
        if (analysis.equals("manual"))
            ManualAnnotations.collectStats(inputdir);
        else if (analysis.equals("agreement"))
            Agreement.agreement(inputdir);
        else
            throw new IllegalArgumentException("Unknown analysis mode: " + analysis);
    }

    private static void runConfiguration(String indir, String inputType,
                                         String outdir, String outputType,
                                         String refDir, String refType,
                                         String entitySource,
                                         String dataVersion, boolean replaceTokens,
                                         boolean addEntities) throws AbnormalProcessException {
        if (outputType.equals(IO.NAF_SFX) && refType.equals(IO.NAF_SFX)) {
            if (inputType.equals(IO.CONLL_SFX)) {
                if (entitySource.equals("man"))
                    IO.loop(indir, Collections.singletonList(refDir), outdir,
                            throwingBiConsumerWrapper((x, y) -> NafXmiReader.runWithConnl2Xmi(x, y)));
                else
                    IO.loop(indir, Collections.singletonList(refDir), outdir,
                            throwingBiConsumerWrapper((x, y) -> NAFConllReader.run(x, y, entitySource, dataVersion, replaceTokens, addEntities)));
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
                                         String selectText,
                                         boolean tsvForTF,
                                         boolean segmentConllByTUnits) throws AbnormalProcessException {
        if (inputType.equals(IO.TEI_SFX) && outputType.equals(IO.NAF_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Tei2Naf.convertFile(x, y)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.NAF_SFX)) {
            if (tokenize)
                IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> NafTokenizer.run(x, y)));
            else
                IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> NafUnitSelector.run(x, y, selectText)));
        } else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.CONLL_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Naf2Conll.run(x, y, segmentConllByTUnits)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.TSV_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Naf2Tsv.run(x, y, tsvForTF)));
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
        options.addOption("d", true, "select units corresponding to: text|notes|all; default: all");
        options.addOption("t", false, "tokenize NAF");
        options.addOption("e", true, "source of input entities (man|corr|sys)");
        options.addOption("v", true, "version input data for NafConllReader");
        options.addOption("w", false, "replace tokens with those of Conll input for NafConllReader");
        options.addOption("n", false, "add new entities to existing entities for NafConllReader");
        options.addOption("f", false, "format TSV for TextFabric");
        options.addOption("u", false, "segment output conll by text units (instead of sentences)");
        options.addOption("a", true, "analysis mode (manual|agreement) -- manual: entity statistics");
        process(options, args);
    }
}
