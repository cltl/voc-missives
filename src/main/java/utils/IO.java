package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class IO {

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
     * @param indir2    input directory
     * @param outdir    output directory
     * @param fileConsumer a bi-consumer (input file from dir1, (indir2 , outdir))
     */
    public static void loop(String indir1, String indir2, String outdir, BiConsumer<Path, List<String>> fileConsumer) {
        Path dirpath = Paths.get(outdir);
        if (!Files.exists(dirpath)) {
            try {
                Files.createDirectories(Paths.get(outdir));
            } catch (IOException e) {
                logger.fatal("Error creating " + outdir, e);
            }
        }
        List<String> dirs = new ArrayList<>();
        dirs.add(indir2);
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

    public static String append(String dirName, String fileName) {
        if (dirName.endsWith("/"))
            return dirName + fileName;
        else
            return dirName + "/" + fileName;
    }
}
