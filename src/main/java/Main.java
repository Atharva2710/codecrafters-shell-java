import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            String[] parts = input.split("\\s+");
            String command = parts[0];
            if (command.equals("exit")) {
                int exitCode = 0;
                if (parts.length > 1) {
                    try {
                        exitCode = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        
                    }
                }
                System.exit(exitCode);
            } else if (command.equals("echo")) {
                if (input.startsWith("echo ")) {
                    System.out.println(input.substring(5));
                } else {
                    System.out.println();
                }
            } else if (command.equals("type")) {
                if (parts.length > 1) {
                    String target = parts[1];
                    if (isBuiltin(target)) {
                        System.out.println(target + " is a shell builtin");
                    } else {
                        System.out.println(target + ": not found");
                    }
                }
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }

    private static boolean isBuiltin(String command) {
        return command.equals("exit") || command.equals("echo") || command.equals("type");
    }
}
