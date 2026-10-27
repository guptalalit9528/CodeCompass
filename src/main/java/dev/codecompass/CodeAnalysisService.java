package dev.codecompass;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CodeAnalysisService {

    // Regex patterns for Java static AST decomposition
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+);", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*(?:public|protected|private|abstract|final|static)*\\s*(class|interface|enum|record)\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern METHOD_PATTERN = Pattern.compile("^\\s*(?:public|protected|private|static|final|synchronized|abstract)*\\s*([\\w<>\\[\\], ]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:throws [\\w, ]+)?\\s*\\{?", Pattern.MULTILINE);
    private static final Pattern CONTROL_PATTERN = Pattern.compile("\\b(if|else if|for|while|switch|try|catch|finally|return|throw)\\b");

    public AdvancedDtos.AstAnalysisResponse parseAst(String code, String language) {
        String lang = language != null && !language.isBlank() ? language.toLowerCase() : "java";
        List<AdvancedDtos.AstNodeDto> nodes = new ArrayList<>();

        if (code == null || code.isBlank()) {
            return new AdvancedDtos.AstAnalysisResponse(lang, false, List.of(), "No code provided for analysis.");
        }

        String[] lines = code.split("\\r?\\n");

        // 1. Package
        Matcher pkgMatcher = PACKAGE_PATTERN.matcher(code);
        if (pkgMatcher.find()) {
            int line = findLineNumber(lines, pkgMatcher.group(0));
            nodes.add(new AdvancedDtos.AstNodeDto("PACKAGE", pkgMatcher.group(1), line, line, List.of(), Map.of()));
        }

        // 2. Imports
        Matcher impMatcher = IMPORT_PATTERN.matcher(code);
        List<AdvancedDtos.AstNodeDto> importNodes = new ArrayList<>();
        while (impMatcher.find()) {
            int line = findLineNumber(lines, impMatcher.group(0));
            importNodes.add(new AdvancedDtos.AstNodeDto("IMPORT", impMatcher.group(1), line, line, List.of(), Map.of()));
        }
        if (!importNodes.isEmpty()) {
            nodes.add(new AdvancedDtos.AstNodeDto("IMPORT_GROUP", importNodes.size() + " Imports",
                    importNodes.get(0).lineStart(), importNodes.get(importNodes.size() - 1).lineEnd(), importNodes, Map.of()));
        }

        // 3. Classes / Interfaces / Records
        Matcher typeMatcher = TYPE_PATTERN.matcher(code);
        while (typeMatcher.find()) {
            String kind = typeMatcher.group(1).toUpperCase();
            String name = typeMatcher.group(2);
            int startLine = findLineNumber(lines, typeMatcher.group(0));
            int endLine = findClosingBraceLine(lines, startLine);

            List<AdvancedDtos.AstNodeDto> members = new ArrayList<>();

            // Find methods inside class
            Matcher mMatcher = METHOD_PATTERN.matcher(code);
            while (mMatcher.find()) {
                String returnType = mMatcher.group(1).trim();
                String methodName = mMatcher.group(2);
                String params = mMatcher.group(3).trim();

                int mStart = findLineNumber(lines, mMatcher.group(0));
                if (mStart >= startLine && mStart <= endLine && !methodName.equals("if") && !methodName.equals("while")) {
                    int mEnd = findClosingBraceLine(lines, mStart);

                    // Check for control flow structures inside method
                    List<AdvancedDtos.AstNodeDto> controlNodes = new ArrayList<>();
                    for (int i = mStart; i < Math.min(lines.length, mEnd); i++) {
                        Matcher cMatch = CONTROL_PATTERN.matcher(lines[i]);
                        while (cMatch.find()) {
                            controlNodes.add(new AdvancedDtos.AstNodeDto("CONTROL_FLOW", cMatch.group(1), i + 1, i + 1, List.of(),
                                    Map.of("statement", lines[i].trim())));
                        }
                    }

                    members.add(new AdvancedDtos.AstNodeDto("METHOD", methodName + "(" + params + ")",
                            mStart, mEnd, controlNodes, Map.of("returns", returnType)));
                }
            }

            nodes.add(new AdvancedDtos.AstNodeDto(kind, name, startLine, endLine, members, Map.of("type", kind)));
        }

        String summary = String.format("Parsed %d top-level declaration(s) across %d lines of %s.",
                nodes.size(), lines.length, lang.toUpperCase());

        return new AdvancedDtos.AstAnalysisResponse(lang, true, nodes, summary);
    }

    public AdvancedDtos.DiffAnalysisResponse computeDiff(String originalCode, String modifiedCode, String filename) {
        String fname = filename != null && !filename.isBlank() ? filename : "Patch.java";
        String[] orig = (originalCode != null ? originalCode : "").split("\\r?\\n");
        String[] mod = (modifiedCode != null ? modifiedCode : "").split("\\r?\\n");

        List<AdvancedDtos.DiffLineDto> lines = new ArrayList<>();
        StringBuilder patch = new StringBuilder();
        patch.append("--- a/").append(fname).append("\n");
        patch.append("+++ b/").append(fname).append("\n");
        patch.append("@@ -1,").append(orig.length).append(" +1,").append(mod.length).append(" @@\n");

        int additions = 0;
        int deletions = 0;

        int i = 0, j = 0;
        while (i < orig.length || j < mod.length) {
            if (i < orig.length && j < mod.length && orig[i].equals(mod[j])) {
                lines.add(new AdvancedDtos.DiffLineDto("EQUAL", i + 1, j + 1, orig[i]));
                patch.append(" ").append(orig[i]).append("\n");
                i++;
                j++;
            } else if (j < mod.length && (i >= orig.length || !containsLater(orig, i, mod[j]))) {
                lines.add(new AdvancedDtos.DiffLineDto("ADD", -1, j + 1, mod[j]));
                patch.append("+").append(mod[j]).append("\n");
                additions++;
                j++;
            } else if (i < orig.length) {
                lines.add(new AdvancedDtos.DiffLineDto("DELETE", i + 1, -1, orig[i]));
                patch.append("-").append(orig[i]).append("\n");
                deletions++;
                i++;
            }
        }

        return new AdvancedDtos.DiffAnalysisResponse(fname, patch.toString(), additions, deletions, lines);
    }

    private boolean containsLater(String[] arr, int start, String target) {
        for (int k = start; k < Math.min(arr.length, start + 5); k++) {
            if (arr[k].equals(target)) return true;
        }
        return false;
    }

    private int findLineNumber(String[] lines, String snippet) {
        String first = snippet.split("\\r?\\n")[0].trim();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(first)) return i + 1;
        }
        return 1;
    }

    private int findClosingBraceLine(String[] lines, int startLine) {
        int depth = 0;
        boolean foundOpen = false;
        for (int i = Math.max(0, startLine - 1); i < lines.length; i++) {
            for (char c : lines[i].toCharArray()) {
                if (c == '{') { depth++; foundOpen = true; }
                else if (c == '}') { depth--; }
            }
            if (foundOpen && depth <= 0) return i + 1;
        }
        return Math.min(lines.length, startLine + 10);
    }
}
