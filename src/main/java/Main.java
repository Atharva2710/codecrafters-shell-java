import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Track the current working directory for the Navigation module
        String currentDirectory = System.getProperty("user.dir");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            // ==========================================
            // MODULE: Quoting & Parsing
            // ==========================================
            // Parse the command line string, respecting single quotes
            List<String> parsedArgs = parseCommandLine(input);
            if (parsedArgs.isEmpty()) {
                continue;
            }
            String command = parsedArgs.get(0);

            // ==========================================
            // MODULE: Base Shell Stages
            // ==========================================

            if (command.equals("exit")) {
                int exitCode = 0;
                if (parsedArgs.size() > 1) {
                    try {
                        exitCode = Integer.parseInt(parsedArgs.get(1));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                System.exit(exitCode);
            } 
            
            else if (command.equals("echo")) {
                // Echo all parsed arguments separated by a single space
                List<String> echoArgs = parsedArgs.subList(1, parsedArgs.size());
                System.out.println(String.join(" ", echoArgs));
            } 
            
            else if (command.equals("type")) {
                if (parsedArgs.size() > 1) {
                    String target = parsedArgs.get(1);
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
            } 

            // ==========================================
            // MODULE: Navigation Extension
            // ==========================================

            else if (command.equals("pwd")) {
                System.out.println(currentDirectory);
            } 
            
            else if (command.equals("cd")) {
                String targetDir = "~";
                if (parsedArgs.size() > 1) {
                    targetDir = parsedArgs.get(1);
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
            } 

            // ==========================================
            // MODULE: Base Shell Stages (External Commands Execution)
            // ==========================================

            else {
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
                        // Spawn external process with arguments and inherit I/O
                        ProcessBuilder pb = new ProcessBuilder(parsedArgs);
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

    // Helper method for resolving system PATH executables
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

    // Helper method to parse the command line string, respecting single quotes
    private static List<String> parseCommandLine(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inArg = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuotes) {
                if (c == '\'') {
                    inSingleQuotes = false;
                    inArg = true;
                } else {
                    currentArg.append(c);
                    inArg = true;
                }
            } else {
                if (c == '\'') {
                    inSingleQuotes = true;
                    inArg = true;
                } else if (Character.isWhitespace(c)) {
                    if (inArg) {
                        args.add(currentArg.toString());
                        currentArg.setLength(0);
                        inArg = false;
                    }
                } else {
                    currentArg.append(c);
                    inArg = true;
                }
            }
        }
        if (inArg) {
            args.add(currentArg.toString());
        }
        return args;
    }
}
