package physics;

import java.util.Map;

// This basically takes a math equation written as a string and evaluates it
// in to a number, basically a mini calculator that can also take in to account
// variables and named constants

public class ExpressionParser {

    public double evaluate(String equation, String[] names, double[] state, Map<String, Double> constants) {

        // we are using whole word matching because if we just did replace
        // of for example t with 3 and we had time then the time would become 3ime and
        // we dont want that

        // step 1: replace constants using whole word matching
        // we created the hashmap when asking the user for the input
        // so here now we are replacing the key (A for example) (entry.getKey()) with
        // it's
        // value for example 0.15 (entry.getValue()) this happens through
        // a set basically the set is kinda like an array but can contain multiple
        // things
        // at an index slot so index 1 can contain a and 0.15 so then when we do
        // entry get key we get that set indexes key and the same for the value
        // so then when we do replace we replace the the key with the value and once
        // that is done
        // we move onto the next index of the set until we are done and once we are done
        // all the constants
        // are replaced
        for (Map.Entry<String, Double> entry : constants.entrySet()) {
            equation = equation.replaceAll(
                    "(?<![a-zA-Z0-9.])" + entry.getKey() + "(?![a-zA-Z0-9.])",
                    String.valueOf(entry.getValue()));
        }

        // step 2: replace variables using whole word matching
        // here we are doing kind of the same thing but with arrays since
        // the variables will be changing so we dont want to put them in a hashmap
        // the i is the same for the names and state so the same one will be replaced
        for (int i = 0; i < names.length; i++) {
            equation = equation.replaceAll(
                    "(?<![a-zA-Z0-9.])" + names[i] + "(?![a-zA-Z0-9.])",
                    String.valueOf(state[i]));
        }

        // step 3: remove all spaces
        equation = equation.replace(" ", "");

        // step 4: calculate the result
        return calculate(equation);
    }

    private double calculate(String equation) {

        // this is basically how the calculate method works:
        // lets say we have 2x3+ 7 now since there exists order of operations we have to
        // do
        // the multiplcation first so we split into 2x3 and 7, there are no conditions
        // to stop
        // 7 from going to the parse in to double thing so 7 becomes a double then 2x3
        // will hit the
        // multiplication call creating another call and splitting them in to 2 and 3
        // then there is nothing
        // stopping them from hitting the parse in to double thing so they become 2 and
        // 3 as doubles and then
        // we have to finish the leftover calls the multiplication one so we do left
        // times right
        // getting 6 since 2 x 3 = 6 and then we have one left over call which was the
        // addition call
        // so we get left + right so 6 +7 = 13 and then we return 13

        // ---- handle named math functions ----
        // we check for these FIRST before any operator scanning, because something like
        // sin(0.5+0.5) needs to be recognised as a function call, not scanned for +
        // operators
        // the pattern is: does the string start with "functionname(" and end with ")"?
        // if yes, and the closing ) belongs to this function (not something nested
        // inside),
        // then we strip the function name, evaluate the inside, and apply the math
        // function
        String[] funcs = { "sin", "cos", "tan", "sqrt", "abs", "exp", "log" };
        for (String fn : funcs) {
            if (equation.startsWith(fn + "(") && equation.endsWith(")")) {
                // check the closing ) actually belongs to this function call
                // and not just coincidentally at the end of something like sin(x)+cos(y)
                // we do this by finding the matching ) for the opening ( right after the fn
                // name
                int openParen = fn.length(); // index of the '('
                if (matchingParen(equation, openParen) == equation.length() - 1) {
                    // everything between the parens is the argument - evaluate it recursively
                    double inner = calculate(equation.substring(openParen + 1, equation.length() - 1));
                    switch (fn) {
                        case "sin":
                            return Math.sin(inner);
                        case "cos":
                            return Math.cos(inner);
                        case "tan":
                            return Math.tan(inner);
                        case "sqrt":
                            return Math.sqrt(inner);
                        case "abs":
                            return Math.abs(inner);
                        case "exp":
                            return Math.exp(inner);
                        case "log":
                            return Math.log(inner);
                    }
                }
            }
        }

        // handle + and - last (lowest priority)
        // we scan right to left so that we respect left-associativity (e.g. 5-3-1 =
        // (5-3)-1 = 1, not 5-(3-1) = 3)
        // the depth tracking works as follows: since we scan right to left, a ')' means
        // we are
        // entering a bracket group (going inward) so depth goes up, and '(' means we
        // are leaving
        // one so depth goes down - we only split on operators that are outside all
        // brackets (depth == 0)
        int depth = 0;
        for (int i = equation.length() - 1; i >= 0; i--) {
            char c = equation.charAt(i);
            if (c == ')')
                depth++; // going right-to-left, ')' opens a bracket group
            if (c == '(')
                depth--; // going right-to-left, '(' closes a bracket group
            if (depth == 0 && (c == '+' || c == '-') && i > 0) {
                // make sure this is a binary operator, not a unary minus
                // a unary minus would follow another operator or an opening paren
                // a binary one follows a digit or closing paren
                char prev = equation.charAt(i - 1);
                if (Character.isDigit(prev) || prev == ')') {
                    double left = calculate(equation.substring(0, i));
                    double right = calculate(equation.substring(i + 1));
                    return c == '+' ? left + right : left - right;
                }
            }
        }

        // reset depth before second loop
        depth = 0;

        // handle * and / (higher priority)
        for (int i = equation.length() - 1; i >= 0; i--) {
            char c = equation.charAt(i);
            if (c == ')')
                depth++; // going right-to-left, ')' opens a bracket group
            if (c == '(')
                depth--; // going right-to-left, '(' closes a bracket group
            if (depth == 0 && (c == '*' || c == '/') && i > 0) {
                double left = calculate(equation.substring(0, i));
                double right = calculate(equation.substring(i + 1));
                return c == '*' ? left * right : left / right;
            }
        }

        // reset depth before third loop
        depth = 0;

        // handle ^ for exponentiation (highest priority among binary operators)
        // we scan left to right here so that ^ is right-associative, meaning 2^3^2
        // is treated as 2^(3^2) = 2^9 = 512, which is the standard mathematical
        // convention
        for (int i = 0; i < equation.length(); i++) {
            char c = equation.charAt(i);
            if (c == '(')
                depth++; // going left-to-right, '(' opens a bracket group
            if (c == ')')
                depth--; // going left-to-right, ')' closes a bracket group
            if (depth == 0 && c == '^' && i > 0) {
                double left = calculate(equation.substring(0, i));
                double right = calculate(equation.substring(i + 1));
                return Math.pow(left, right);
            }
        }

        // handle brackets: if the whole equation is wrapped in brackets, strip them and
        // recurse
        // for example (2+3) becomes 2+3 and then gets evaluated normally
        if (equation.startsWith("(") && equation.endsWith(")")) {
            return calculate(equation.substring(1, equation.length() - 1));
        }

        // handle unary minus: e.g. -5 or -(2+3)
        // this comes after operator scanning so we dont accidentally treat a binary
        // minus as unary
        if (equation.startsWith("-")) {
            return -calculate(equation.substring(1));
        }

        // no operators left - just a number
        try {
            return Double.parseDouble(equation);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot parse token: '" + equation + "' — check your equation for typos.");
        }
    }

    // given the index of an opening '(' in the string, finds the index of its
    // matching ')'
    // we need this to check whether the closing ) of something like sin(...)
    // actually
    // belongs to that function call or is just coincidentally at the end of the
    // string
    private int matchingParen(String expr, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < expr.length(); i++) {
            if (expr.charAt(i) == '(')
                depth++;
            if (expr.charAt(i) == ')')
                depth--;
            if (depth == 0)
                return i;
        }
        throw new IllegalArgumentException("Unbalanced parentheses in: " + expr);
    }
}
