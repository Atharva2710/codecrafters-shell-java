import java.io.File;
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
            
            // Check and run built-in shell commands
            if (command.equals("exit")) {
                int exitCode = 0;
                if (parts.length > 1) {
                    try {
                        exitCode = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        // ignore
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
                        // Check if command is a valid executable in the system PATH
                        String path = getPathOfExecutable(target);
                        if (path != null) {
                            System.out.println(target + " is " + path);
                        } else {
                            System.out.println(target + ": not found");
                        }
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

    // Resolves executable files by searching directories listed in the PATH environment variable
    private static String getPathOfExecutable(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }
        String[] directories = pathEnv.split(File.pathSeparator);
        for (String dir : directories) {
            File file = new File(dir, command);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}
