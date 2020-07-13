package missives;

import entities.nafReference.NAFConllReader;
import entities.nafReference.Naf2Conll;
import entities.nafReference.NafXmiReader;
import entities.rawTextAligner.EntityOffsetAligner;
import org.apache.commons.cli.*;
import entities.xmiReference.EntityAligner;
import utils.common.IO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;
import static text.tei2naf.NafConverter.convertFile;
import static entities.xmiReference.Conll.run;

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
            String inputType = cmd.hasOption("I") ? cmd.getOptionValue('I') : inferType(indir);
            if (! isKnownType(inputType))
                throw new IllegalArgumentException("input type " + inputType + " is invalid. Select one type among xml (tei), naf, entities.xmi or conll.");

            final String outdir = cmd.hasOption('o') ? cmd.getOptionValue('o') : "";
            String outputType = cmd.hasOption("O") ? cmd.getOptionValue('O') : IO.NAF_SFX;
            String conllSeparator = cmd.hasOption("c") ? cmd.getOptionValue('c') : " ";
            String selectText = cmd.hasOption("s") ? cmd.getOptionValue("s") : "mixed";
            String source = cmd.hasOption("e") ? cmd.getOptionValue("e") : "voc-missives-naf-conll-reader";
            if (cmd.hasOption('r') || cmd.hasOption('R')) {
                final String refdir = cmd.getOptionValue('r');
                String refType = cmd.hasOption("R") ? cmd.getOptionValue('R') : inferType(refdir);
                if (! (refType.equals(IO.NAF_SFX) || refType.equals(IO.XMI_SFX)))
                    throw new IllegalArgumentException("Invalid reference type. Select one of entities.xmi or naf.");
                runConfiguration(indir, inputType, outdir, outputType, refdir, refType, conllSeparator, selectText, source);
            } else
                runConfiguration(indir, inputType, outdir, outputType, cmd.hasOption('t'), conllSeparator, selectText);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            usage(options);
        }
    }

    private static void runConfiguration(String indir, String inputType,
                                         String outdir, String outputType,
                                         String refDir, String refType,
                                         String conllSeparator,
                                         String selectText,
                                         String source) {
        if (inputType.equals(IO.XMI_SFX) && outputType.equals(IO.XMI_SFX) && refType.equals(IO.XMI_SFX))
            IO.loop(indir, Collections.singletonList(refDir), outdir,
                throwingBiConsumerWrapper((x, y) -> EntityAligner.run(x, y)));
        else if (inputType.equals(IO.CONLL_SFX) && outputType.equals(IO.NAF_SFX) && refType.equals(IO.NAF_SFX))
            IO.loop(indir, Collections.singletonList(refDir), outdir,
                    throwingBiConsumerWrapper((x, y) -> NAFConllReader.run(x, y, selectText, conllSeparator, source)));
        else if (inputType.equals(IO.XMI_SFX) && outputType.equals(IO.NAF_SFX) && refType.equals(IO.NAF_SFX))
            IO.loop(indir, Collections.singletonList(refDir), outdir,
                    throwingBiConsumerWrapper((x, y) -> NafXmiReader.run(x, y, selectText, source)));
        else if (inputType.equals(IO.XMI_SFX) && outputType.equals(IO.XMI_SFX) && refType.equals(IO.NAF_SFX)) {
            IO.loop(indir, Collections.singletonList(refDir), outdir,
                    throwingBiConsumerWrapper((x, y) -> EntityOffsetAligner.run(x, y)));
            EntityOffsetAligner.finalStats();
        }
        else
            throw new IllegalArgumentException("annotations integration is currently only supported for CAS XMI and Conll2Naf");
    }

    private static void runConfiguration(String indir,
                                         String inputType,
                                         String outdir,
                                         String outputType,
                                         boolean tokenize,
                                         String conllSeparator,
                                         String selectText) {
        if (inputType.equals(IO.TEI_SFX)) {
            if (outputType.equals(IO.NAF_SFX))
                IO.loop(indir, outdir,
                        throwingBiConsumerWrapper((x, y) -> convertFile(x, y, tokenize)));
            else if (outputType.equals(IO.XMI_SFX))    // documents are always tokenized
                IO.loop(indir, outdir,
                        throwingBiConsumerWrapper((x, y) -> text.tei2xmi.Converter.convertFile(x, y)));
        } else if (inputType.equals(IO.XMI_SFX) && outputType.equals(IO.CONLL_SFX))
                IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> run(x, y, conllSeparator)));
        else if (inputType.equals(IO.NAF_SFX) && outputType.equals(IO.CONLL_SFX))
            IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> Naf2Conll.run(x, y, conllSeparator, selectText)));
        else
            throw new IllegalArgumentException("conversion from " + inputType + " to " + outputType + " is not supported");
    }

    private static boolean isKnownType(String t) {
        return t.equals(IO.NAF_SFX) || t.equals(IO.TEI_SFX) || t.equals(IO.CONLL_SFX) || t.equals(IO.XMI_SFX);
    }

    private static String inferType(String indir) {
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            Path file = paths.filter(Files::isRegularFile).findAny().orElse(null);
            String fileName = file.getFileName().toString();
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Cannot infer type from files in " + indir);
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", true, "input file / directory");
        options.addOption("o", true, "output directory. Defaults to current directory");
        options.addOption("r", true, "reference directory for NE annotations integration");
        options.addOption("I", true, "input file type (xml|naf|conll|xmi); inferred by default from input files extension: 'xml' (TEI files), 'naf', 'xmi' or 'conll'");
        options.addOption("O", true, "output file type; default: NAF");
        options.addOption("R", true, "reference file type (naf|xmi); inferred by default from reference files extension ");
        options.addOption("c", true, "conll separator for Conll output; defaults to single space");
        options.addOption("s", true, "selected text type for Conll output and entity integration: text|notes|mixed|all; default:mixed");
        options.addOption("e", true, "source of entity annotations (for NAF header: linguistic processor info)");
        options.addOption("t", false, "segment and tokenize");
        process(options, args);
    }
}
