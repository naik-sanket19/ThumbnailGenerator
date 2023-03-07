/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.naik.sanket19.ThumbnailGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Stream;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 *
 * @author sanketm
 */
@SpringBootApplication
public class Main implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String THUMBNAIL = ".thumbnail";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final String path = String.join(" ", args.getNonOptionArgs());
        log.info("{}", path);
        createThumbnail(new File(path));
    }

    private void createThumbnail(File path) throws IOException {
        log.info("checking folder: {}", path);
        File destinationDir = new File(path, THUMBNAIL);
        
        if (!destinationDir.exists()) {
            destinationDir.mkdir();
        }

        var dirs = new LinkedList<File>();
        var images = new LinkedList<File>();
        try (Stream<Path> input = Files.list(path.toPath())) {
            input
                    .map(Path::toFile)
                    .filter(Main::isNotThumbnail)
                    .filter(f -> {
                        if (f.isDirectory()) {
                            synchronized(dirs){
                                dirs.add(f);
                            }
                            return false;
                        }
                        return true;
                    })
                    .forEach(f -> images.add(f));
        }

        images.stream()
                .parallel()
                .filter(Main::isImage)
                    .filter(f -> thumbnailNotExist(f, destinationDir))
                    .forEach(path1 -> {
                        try {
                            generateThumbnail(path1, destinationDir);
                        } catch (IOException ex) {
                            log.error(path1.toString(), ex);
                        }
                    });
        dirs.forEach(f -> {
            try {
                createThumbnail(f);
            } catch (IOException ex) {
                log.error(f.getAbsolutePath(), ex);
            }
        });
    }

    private static boolean thumbnailNotExist(File main, File destDir) {
        return !(new File(destDir, main.getName()).exists());
    }

    private static boolean isNotThumbnail(File p) {
        return !(p.getName().endsWith(THUMBNAIL));
    }

    private static boolean isImage(File path) {
        return (path.getName().endsWith("jpg")
                || path.getName().endsWith("jpeg")
                || path.getName().endsWith("JPEG")
                || path.getName().endsWith("JPG")
                || path.getName().endsWith("png"));
    }

    private void generateThumbnail(File path, File dest) throws IOException {
        log.info("Creating thumbnail: {}", path);
        Thumbnails.of(path)
                .size(400, 400)
                .toFiles(dest, Rename.NO_CHANGE);
    }
}
