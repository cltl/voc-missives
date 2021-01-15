package utils.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IO {

    public static final String NAF_SFX = "naf";
    public static final String TEI_SFX = "tei";
    public static final String XMI_SFX = "xmi";
    public static final String CONLL_SFX = "conll";
    private static final Pattern INT_ID = Pattern.compile("INT_[a-z0-9-]+");
    private static final Logger logger = LogManager.getLogger(IO.class);
    /**
     * Processes files in dir given a file consumer
     * @param indir     input directory
     * @param outdir    output directory
     * @param fileConsumer a bi-consumer (input file path, output directory)
     */
    public static void loop(String indir, String outdir, BiConsumer<Path, String> fileConsumer) {
        Path dirpath = Paths.get(outdir);
        if (!Files.exists(dirpath)) {
            try {
                Files.createDirectories(Paths.get(outdir));
            } catch (IOException e) {
                logger.fatal("Error creating " + outdir, e);
            }
        }
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            paths.filter(p -> Files.isRegularFile(p)).filter(p -> {
                try {
                    return ! Files.isHidden(p);
                } catch (IOException e) {
                    logger.warn("Error testing " + p, e);
                    return false;
                }}).forEach(f -> fileConsumer.accept(f, outdir));
        } catch (IOException e) {
            logger.fatal("Error processing files in " + indir, e);
        }
    }
    /**
     * Processes files in dir given a file consumer
     * @param indir1    input directory
     * @param auxdirs   auxilliary input directories
     * @param outdir    output directory
     * @param fileConsumer a bi-consumer (input file from dir1, (indir2 , outdir))
     */
    public static void loop(String indir1, List<String> auxdirs, String outdir, BiConsumer<Path, List<String>> fileConsumer) {
        Path dirpath = Paths.get(outdir);
        if (!Files.exists(dirpath)) {
            try {
                Files.createDirectories(dirpath);
            } catch (IOException e) {
                logger.fatal("Error creating " + outdir, e);
            }
        }
        List<String> dirs = new ArrayList<>();
        dirs.addAll(auxdirs);
        dirs.add(outdir);
        try (Stream<Path> paths = Files.walk(Paths.get(indir1))) {
            paths.filter(p -> Files.isRegularFile(p)).filter(p -> {
                try {
                    return ! Files.isHidden(p);
                } catch (IOException e) {
                    logger.warn("Error testing " + p, e);
                    return false;
                }}).forEach(f -> fileConsumer.accept(f, dirs));

        } catch (IOException e) {
            logger.fatal("Error processing files in " + indir1, e);
        }

    }

    public static void loopToFile(String indir, String outFile, BiConsumer<Path,BufferedWriter> fileConsumer) {
        try (Stream<Path> paths = Files.walk(Paths.get(indir));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            paths.filter(p -> Files.isRegularFile(p)).filter(p -> {
                try {
                    return ! Files.isHidden(p);
                } catch (IOException e) {
                    logger.warn("Error testing " + p, e);
                    return false;
                }}).forEach(f -> fileConsumer.accept(f, bw));

        } catch (IOException e) {
            logger.fatal("Error processing files in " + indir, e);
        }
    }

    public static String getTargetFile(String targetDir, Path file, String oldExtension, String newExtension) {
        return Paths.get(targetDir, file.getFileName().toString().replaceAll(oldExtension, newExtension)).toString();
    }

    public static String inferType(String indir) {
        try (Stream<Path> paths = Files.walk(Paths.get(indir))) {
            Path file = paths.filter(Files::isRegularFile).findAny().orElse(null);
            return extension(file.getFileName().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Cannot infer type from files in " + indir);
    }

    public static String extension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private static String getId(String fileName) throws AbnormalProcessException {
        Matcher matcher = INT_ID.matcher(fileName);
        if (matcher.find())
            return matcher.group();
        else
            throw new AbnormalProcessException("File name does not match INT id pattern.");
    }

    /**
     * matches the file id of <code>path</code> against that of files in <code>path</code>
     * @param path
     * @param dir
     * @return  the matching file
     * @throws AbnormalProcessException
     */
    public static File findFileWithSameId(Path path, File dir) throws AbnormalProcessException {
        String id = getId(path.getFileName().toString());
        List<File> matching = new LinkedList<>();
        for (File f: dir.listFiles()) {
            if (id.equals(getId(f.getName())))
                matching.add(f);
        }
        if (matching.isEmpty())
            throw new AbnormalProcessException("Directory " + dir.getPath() + " does not contain any file with id: " + id);
        if (matching.size() > 1)
            throw new AbnormalProcessException("Directory " + dir.getPath() + " contains several files with id: " + id
                    + "\n" + matching.stream().map(f -> f.getName()).collect(Collectors.joining(",")));
        return matching.get(0);
    }
}
