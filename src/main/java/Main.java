import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        String currentDirectory = System.getProperty("user.dir");
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
            } else if (command.equals("pwd")) {
                System.out.println(currentDirectory);
            } else if (command.equals("cd")) {
                String targetDir = "~";
                if (parts.length > 1) {
                    targetDir = parts[1];
                }

                File dir;
                if (targetDir.startsWith("/")) {
                    dir = new File(targetDir);
                } else if (targetDir.startsWith("~")) {
                    String home = System.getenv("HOME");
                    if (home == null) {
                        home = System.getProperty("user.home");
                    }
                    String path = targetDir.replaceFirst("^~", home);
                    dir = new File(path);
                } else {
                    dir = new File(currentDirectory, targetDir);
                }

                if (dir.exists() && dir.isDirectory()) {
                    try {
                        currentDirectory = dir.getCanonicalPath();
                    } catch (Exception e) {
                        currentDirectory = dir.getAbsolutePath();
                    }
                } else {
                    System.out.println("cd: " + targetDir + ": No such file or directory");
                }
            } else {
                // Determine if the command is an executable in PATH or direct file path
                String path = null;
                if (command.contains("/") || command.contains(File.separator)) {
                    File file = new File(command);
                    if (file.exists() && file.isFile() && file.canExecute()) {
                        path = file.getAbsolutePath();
                    }
                } else {
                    path = getPathOfExecutable(command);
                }

                if (path != null) {
                    try {
                        // Spawn external process with arguments and wait for completion
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        pb.directory(new File(currentDirectory));
                        pb.inheritIO();
                        Process process = pb.start();
                        process.waitFor();
                    } catch (Exception e) {
                        System.out.println(input + ": command not found");
                    }
                } else {
                    System.out.println(input + ": command not found");
                }
            }
        }
    }

    private static boolean isBuiltin(String command) {
        return command.equals("exit") || command.equals("echo") || command.equals("type") || command.equals("pwd") || command.equals("cd");
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
