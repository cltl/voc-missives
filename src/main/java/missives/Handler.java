package missives;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.ObjectUtils;
import xmi.EntityAligner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import static missives.ThrowingBiConsumer.throwingBiConsumerWrapper;
import static tei2naf.NafConverter.convertFile;
import static xmi.Conll.run;

/**
 * Main class for missives processing.
 *
 */
public class Handler {
    public static final String NAF_EXT = "naf";
    public static final String TEI_EXT = "xml";
    public static final String XMI_EXT = "xmi";
    public static final String CONLL_EXT = "conll";


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
                throw new IllegalArgumentException("input type " + inputType + " is invalid. Select one type among xml (tei), naf, xmi or conll.");

            final String outdir = cmd.hasOption('o') ? cmd.getOptionValue('o') : "";
            String outputType = cmd.hasOption("O") ? cmd.getOptionValue('O') : NAF_EXT;
            String conllSeparator = cmd.hasOption("s") ? cmd.getOptionValue('s') : " ";
            if (cmd.hasOption('r') || cmd.hasOption('R')) {
                final String refdir = cmd.getOptionValue('r');
                String refType = cmd.hasOption("R") ? cmd.getOptionValue('R') : inferType(refdir);
                if (! (refType.equals(NAF_EXT) || refType.equals(XMI_EXT)))
                    throw new IllegalArgumentException("Invalid reference type. Select one of xmi or naf.");
                runConfiguration(indir, inputType, outdir, outputType, refdir, refType);
            } else
                runConfiguration(indir, inputType, outdir, outputType, cmd.hasOption('t'), conllSeparator);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            usage(options);
        }
    }

    private static void runConfiguration(String indir, String inputType, String outdir, String outputType, String refDir, String refType) {
        if (inputType.equals(XMI_EXT) && outputType.equals(XMI_EXT) && refType.equals(XMI_EXT))
            IO.loop(indir, Collections.singletonList(refDir), outdir,
                throwingBiConsumerWrapper((x, y) -> EntityAligner.run(x, y)));
        throw new IllegalArgumentException("annotations integration is currently only supported for CAS XMI");
        // TODO implement XMI -> NAF
    }

    private static void runConfiguration(String indir, String inputType, String outdir, String outputType, boolean tokenize, String conllSeparator) {
        if (inputType.equals(TEI_EXT)) {
            if (outputType.equals(NAF_EXT))
                IO.loop(indir, outdir,
                        throwingBiConsumerWrapper((x, y) -> convertFile(x, y, tokenize)));
            else if (outputType.equals(XMI_EXT))    // documents are always tokenized
                IO.loop(indir, outdir,
                        throwingBiConsumerWrapper((x, y) -> tei2xmi.Converter.convertFile(x, y)));
            else if (outputType.equals(CONLL_EXT))  // TODO implement
                throw new IllegalArgumentException("direct conversion to conll is not supported yet");
        } else if (inputType.equals(XMI_EXT) && outputType.equals(CONLL_EXT))
                IO.loop(indir, outdir, throwingBiConsumerWrapper((x, y) -> run(x, y, conllSeparator)));
        else
            throw new IllegalArgumentException("conversion from " + inputType + " to " + outputType + " is not supported");
    }

    private static boolean isKnownType(String t) {
        return t.equals(NAF_EXT) || t.equals(TEI_EXT) || t.equals(CONLL_EXT) || t.equals(XMI_EXT);
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
        options.addOption("r", true, "reference directory (for NE annotations integration");
        options.addOption("I", true, "input file type; defaults to the file type read from input files extension (xml|naf|xmi|conll), where xml is the extension for TEI files");
        options.addOption("O", true, "output file type; default: NAF");
        options.addOption("R", true, "reference file type; defaults to the file type read from reference files extension (naf|xmi)");
        options.addOption("s", true, "conll separator for Conll output; defaults to single space");
        options.addOption("t", false, "segment and tokenize");
        process(options, args);
    }
}
