import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ShuntingYard {
    private static final String CONCATENATION = "·";
    private static final String BINARY_OPERATORS = "|·^";
    private static final String POSTFIX_OPERATORS = "?*+";

    private ArrayList<ArrayList<String>> tokens;

    public ShuntingYard() {
        tokens = new ArrayList<>();
    }

    public void tokenize(File file) throws IOException {
        tokens.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ArrayList<String> lineTokens = new ArrayList<>();
                for (char symbol : line.toCharArray()) {
                    if (!Character.isWhitespace(symbol)) {
                        lineTokens.add(String.valueOf(symbol));
                    }
                }
                tokens.add(lineTokens);
            }
        }
    }

    public ArrayList<ArrayList<String>> getTokens() {
        return tokens;
    }

    public int getPrecedence(String token) {
        if (token.equals("(")) {
            return 1;
        }
        if (token.equals("|")) {
            return 2;
        }
        if (token.equals(CONCATENATION)) {
            return 3;
        }
        if (POSTFIX_OPERATORS.contains(token)) {
            return 4;
        }
        if (token.equals("^")) {
            return 5;
        }
        return 0;
    }

    public ArrayList<String> formatRegEx(ArrayList<String> regex) {
        ArrayList<String> grouped = groupEscapedCharacters(regex);
        ArrayList<String> formatted = new ArrayList<>();

        for (int i = 0; i < grouped.size(); i++) {
            String current = grouped.get(i);
            formatted.add(current);
            if (i + 1 < grouped.size()
                    && canEndOperand(current)
                    && canStartOperand(grouped.get(i + 1))) {
                formatted.add(CONCATENATION);
            }
        }
        return formatted;
    }

    public ArrayList<String> infixToPostfix(ArrayList<String> regex) {
        return infixToPostfix(regex, true);
    }

    private ArrayList<String> infixToPostfix(ArrayList<String> regex, boolean trace) {
        ArrayList<String> postfix = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        ArrayList<String> formattedRegEx = formatRegEx(regex);
        boolean expectingOperand = true;

        if (formattedRegEx.isEmpty()) {
            throw new IllegalArgumentException("expresión vacía");
        }
        if (trace) {
            System.out.println("Tokens: " + String.join(" ", formattedRegEx));
            System.out.printf("%-10s %-22s %-35s %s%n",
                    "Token", "Acción", "Salida", "Pila");
        }

        for (String token : formattedRegEx) {
            String action;
            if (isOperand(token)) {
                postfix.add(token);
                expectingOperand = false;
                action = "enviar a salida";
            } else if (token.equals("(")) {
                stack.addLast(token);
                expectingOperand = true;
                action = "apilar";
            } else if (token.equals(")")) {
                if (expectingOperand) {
                    throw new IllegalArgumentException("paréntesis vacío o cierre inesperado");
                }
                int moved = 0;
                while (!stack.isEmpty() && !stack.peekLast().equals("(")) {
                    postfix.add(stack.removeLast());
                    moved++;
                }
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("paréntesis de cierre sin apertura");
                }
                stack.removeLast();
                expectingOperand = false;
                action = "cerrar grupo (" + moved + " movidos)";
            } else {
                if (expectingOperand) {
                    throw new IllegalArgumentException(
                            "operador " + token + " sin expresión previa");
                }
                int moved = 0;
                while (!stack.isEmpty()
                        && !stack.peekLast().equals("(")
                        && getPrecedence(stack.peekLast()) >= getPrecedence(token)) {
                    postfix.add(stack.removeLast());
                    moved++;
                }
                stack.addLast(token);
                expectingOperand = isBinaryOperator(token);
                action = moved == 0 ? "apilar" : "mover " + moved + " y apilar";
            }
            if (trace) {
                printStep(token, action, postfix, stack);
            }
        }

        if (expectingOperand) {
            throw new IllegalArgumentException("la expresión termina con un operador");
        }
        while (!stack.isEmpty()) {
            if (stack.peekLast().equals("(")) {
                throw new IllegalArgumentException("paréntesis de apertura sin cierre");
            }
            postfix.add(stack.removeLast());
            if (trace) {
                printStep("fin", "vaciar pila", postfix, stack);
            }
        }

        return postfix;
    }

    private ArrayList<String> groupEscapedCharacters(ArrayList<String> regex) {
        ArrayList<String> grouped = new ArrayList<>();

        for (int i = 0; i < regex.size(); i++) {
            String token = regex.get(i);
            if (token.equals("\\")) {
                if (++i == regex.size()) {
                    throw new IllegalArgumentException("carácter de escape sin símbolo");
                }
                grouped.add("\\" + regex.get(i));
            } else if (token.equals("[")) {
                ArrayList<String> members = new ArrayList<>();
                boolean closed = false;
                while (++i < regex.size()) {
                    token = regex.get(i);
                    if (token.equals("\\")) {
                        if (++i == regex.size()) {
                            throw new IllegalArgumentException(
                                    "carácter de escape incompleto dentro de []");
                        }
                        members.add("\\" + regex.get(i));
                    } else if (token.equals("]")) {
                        closed = true;
                        break;
                    } else {
                        members.add(token);
                    }
                }
                if (!closed) {
                    throw new IllegalArgumentException("clase de caracteres sin cerrar");
                }
                if (members.isEmpty()) {
                    throw new IllegalArgumentException("clase de caracteres vacía");
                }
                grouped.add("(");
                for (int member = 0; member < members.size(); member++) {
                    if (member > 0) {
                        grouped.add("|");
                    }
                    grouped.add(members.get(member));
                }
                grouped.add(")");
            } else {
                grouped.add(token);
            }
        }
        return grouped;
    }

    private boolean isOperand(String token) {
        return !token.equals("(")
                && !token.equals(")")
                && !isBinaryOperator(token)
                && !isPostfixOperator(token);
    }

    private boolean isBinaryOperator(String token) {
        return token.length() == 1 && BINARY_OPERATORS.contains(token);
    }

    private boolean isPostfixOperator(String token) {
        return token.length() == 1 && POSTFIX_OPERATORS.contains(token);
    }

    private boolean canEndOperand(String token) {
        return isOperand(token) || token.equals(")") || isPostfixOperator(token);
    }

    private boolean canStartOperand(String token) {
        return isOperand(token) || token.equals("(");
    }

    private void printStep(String token, String action, List<String> output,
            Deque<String> stack) {
        System.out.printf("%-10s %-22s %-35s %s%n",
                token, action, String.join(" ", output), stack);
    }

    public static void selfTest() {
        ArrayList<String> expression = new ArrayList<>();
        for (char symbol : "[ae]+".toCharArray()) {
            expression.add(String.valueOf(symbol));
        }
        String postfix = String.join("",
                new ShuntingYard().infixToPostfix(expression, false));
        if (!postfix.equals("ae|+")) {
            throw new AssertionError("se esperaba ae|+, se obtuvo " + postfix);
        }
        System.out.println("Prueba correcta: [ae]+ -> ae|+");
    }

}
