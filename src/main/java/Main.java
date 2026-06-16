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
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}
