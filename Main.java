import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            ShuntingYard.selfTest();
            return;
        }

        ShuntingYard converter = new ShuntingYard();
        File file = new File(args.length > 0 ? args[0] : "regex.txt");

        try {
            converter.tokenize(file);
        } catch (IOException e) {
            System.err.println("Error reading " + file + ": " + e.getMessage());
            return;
        }

        System.out.println("File: " + file);
        for (int i = 0; i < converter.getTokens().size(); i++) {
            ArrayList<String> expression = converter.getTokens().get(i);
            System.out.printf("%nExpression %d: %s%n", i + 1, String.join("", expression));
            try {
                ArrayList<String> postfix = converter.infixToPostfix(expression);
                System.out.println("Postfix: " + String.join("", postfix));
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

}
