package edu.daa.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GraphReader {
    private GraphReader() { }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = nextDataLine(reader);
            if (header == null) throw new IOException("missing graph header");
            String[] parts = header.split("\\s+");
            if (parts.length != 3) throw new IOException("expected: n m directed|undirected");
            int n = parseInt(parts[0], "vertex count");
            int m = parseInt(parts[1], "edge count");
            boolean directed;
            if (parts[2].equalsIgnoreCase("directed")) directed = true;
            else if (parts[2].equalsIgnoreCase("undirected")) directed = false;
            else throw new IOException("graph kind must be directed or undirected");

            Graph.Builder builder = Graph.builder(n, directed);
            for (int i = 0; i < m; i++) {
                String line = nextDataLine(reader);
                if (line == null) throw new IOException("expected " + m + " edges, found " + i);
                String[] edge = line.split("\\s+");
                if (edge.length != 3) throw new IOException("edge must contain: u v w");
                try {
                    builder.addEdge(Integer.parseInt(edge[0]), Integer.parseInt(edge[1]), Integer.parseInt(edge[2]));
                } catch (IllegalArgumentException ex) {
                    throw new IOException("invalid edge at line " + (i + 2) + ": " + line, ex);
                }
            }
            if (nextDataLine(reader) != null) throw new IOException("file contains more than " + m + " edges");
            return builder.build();
        }
    }

    private static String nextDataLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            int comment = line.indexOf('#');
            if (comment >= 0) line = line.substring(0, comment);
            line = line.trim();
            if (!line.isEmpty()) return line;
        }
        return null;
    }

    private static int parseInt(String value, String label) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IOException("invalid " + label + ": " + value, ex);
        }
    }
}
