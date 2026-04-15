package de.flatmap;


import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("=== Uebung ===\n");




        Flux<String> f1 = Flux.using(() -> Files.lines(Path.of("src/main/resources/vornamen.txt")), reactor.core.publisher.Flux::fromStream, Stream::close);
        Flux<String> f2 = Flux.using(() -> Files.lines(Path.of("src/main/resources/nachnamen.txt")), reactor.core.publisher.Flux::fromStream, Stream::close)
                .subscribeOn(Schedulers.boundedElastic()) // Optimiert für blockierendes I/O
                .delayElements(Duration.ofMillis(100));    // Schickt alle 0,5 Sek. einen Namen


        //f1.subscribe(System.out::println);

        Uebungen.run(f1, f2);
        f2.blockLast();

    }


}
