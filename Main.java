import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            ShuntingYard.selfTest();
            SyntaxTree.selfTest();
            return;
        }

        ShuntingYard converter = new ShuntingYard();
        File file = new File(args.length > 0 ? args[0] : "input.txt");

        try {
            converter.tokenize(file);
        } catch (IOException e) {
            System.err.println("Error reading " + file + ": " + e.getMessage());
            return;
        }

        System.out.println("File: " + file);
        Path outputDirectory = Paths.get("trees");
        boolean renderPng = true;
        for (int i = 0; i < converter.getTokens().size(); i++) {
            ArrayList<String> expression = converter.getTokens().get(i);
            System.out.printf("%nExpression %d: %s%n", i + 1, String.join("", expression));
            try {
                ArrayList<String> grouped = converter.groupEscapedCharacters(expression);
                ArrayList<String> postfix = converter.infixToPostfix(grouped);
                System.out.println("Postfix: " + String.join("", postfix));

                Node tree = SyntaxTree.fromPostfix(postfix);
                String name = String.format("expression_%02d", i + 1);
                Path dot = SyntaxTree.writeGraphviz(tree, outputDirectory.resolve(name + ".dot"));
                System.out.println("Tree: " + dot);

                if (renderPng) {
                    try {
                        Path png = outputDirectory.resolve(name + ".png").toAbsolutePath();
                        SyntaxTree.renderPng(dot, png);
                        System.out.println("Image: " + png);
                    } catch (IOException e) {
                        renderPng = false;
                        System.err.println("PNG not generated (" + e.getMessage()
                                + "); DOT files will still be generated.");
                    }
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error writing tree: " + e.getMessage());
            }
        }
    }

}
