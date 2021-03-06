package org.eclipse.che;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class App {
    public static void main( String[] args ) throws IOException {
        String repoPath = System.getenv("GITHUB_WORKSPACE");
        if (repoPath == null) {
            throw new RuntimeException("Env variable \"GITHUB_WORKSPACE\" wasn't set.");
        }

        String excludeListEnv = System.getenv("EXCLUDE_DEPS");
        List<String> excludeList = excludeListEnv != null ? Arrays.asList(excludeListEnv.split(",\\s+")) : Collections.emptyList();

        File goSum = new File(Path.of(repoPath, "go.sum").toUri());
        Collection<String> goSumDeps = readGoSumFile(goSum);

        String patternString = "\\|\\s+\\[(.*)@.*\\]\\(.*\\)\\s.*";
        Pattern pattern = Pattern.compile(patternString);
        File depListFile = new File(Path.of(repoPath,"DEPENDENCIES.md").toUri());
        List<String> definedDeps = readFile(depListFile);

        List<String> declaredDep = new ArrayList<>();
        for (String line: definedDeps) {
            Matcher matcher = pattern.matcher(line);
            boolean matches = matcher.matches();
            if (matches) {
                declaredDep.add(matcher.group(1));
            }
        }

        System.out.println("===================");
        List<String> missedDeps = new ArrayList<>();
        for (String goSumDep: goSumDeps) {
            if (!declaredDep.contains(goSumDep) && !excludeList.contains(goSumDep)) {
                missedDeps.add(goSumDep);
            }
        }
        if (missedDeps.size() > 0) {
            throw new RuntimeException("Add new dependencies to DEPENDENCIES.md file" + missedDeps);
        }
    }

    public static Collection<String> readGoSumFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        InputStream in = new FileInputStream(file);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.split(" ")[0].split("@")[0]);
            }
        }

        return new HashSet<>(lines);
    }

    public static List<String> readFile(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
