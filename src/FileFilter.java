import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileFilter {
    public static final String HOME_PATH = System.getProperty("user.dir");
    private static final Path startPath = Paths.get(HOME_PATH,"testDir");
    private static final BufferedReader CONSOLE = new BufferedReader(new InputStreamReader(System.in));

    private static String outputPath = startPath.toString();
    private static final String inputPath = startPath.toString();
    private static String prefix = "";
    private static boolean readyMode = false;
    private static boolean appendMode = false;
    private static String statsMode = "short";
    private static final List<String> listFiles = new ArrayList<>();

    static private final Stats integerStats = new Stats("Integers");
    private static final Stats floatStats = new Stats("Floats");
    private static final Stats stringStats = new Stats("Strings");

    private static BufferedWriter intWriter = null;
    private static BufferedWriter floatWriter = null;
    private static BufferedWriter stringWriter = null;

    public static void main(String[] args) {

        System.out.println(
                "\t********************************************************\n" +
                "\t Вас приветствует утилита фильтрации содержимого файлов\n" +
                "\t********************************************************\n" +
                " - Путь к расположению фалов для фильтрации по умолчанию: " + inputPath + "\n"+
                " - Список доступных команд:\n" +
                "\t \"-q\": начать фильтрацию файлов\n " +
                "\t \"-e\": выбрать файлы для фильтрации\n " +
                "\t \"-o\": задать путь для результатов\n " +
                "\t \"-p\": задать префикс имен выходных файлов\n " +
                "\t \"-a\": режим добавления в файл (без перезаписи)\n " +
                "\t \"-s\": выводить краткую статистику\n" +
                "\t \"-f\": выводить полную статистику "
        );
        parseFile();
        parseArgs();

    }

    public static void parseFile() {
        System.out.println( " - Введите имена файлов через запятую:\n");
        if (!listFiles.isEmpty()) {
            listFiles.clear();
        }
        try {
            String line = CONSOLE.readLine();
            String[] splitLine = line.split("\\s*,\\s*");
            listFiles.addAll(Arrays.asList(splitLine));
        } catch (Exception e) {
            System.out.println(" Ошибка поиска: " + e.getMessage());
        }
        for (String listFile : listFiles) {
            Path existFile = Paths.get(inputPath, listFile);
            if (!Files.isRegularFile(existFile)) {
                System.out.println("Файл не найден: " + existFile.toString());
            }else{
                System.out.println("Файл добавлен: " + existFile.toString());
            }
        }
        listFiles.removeIf(s -> !Files.isRegularFile(Paths.get(inputPath, s)));
        readyMode = !listFiles.isEmpty();
    }

    private static void parseArgs() {
        String args = "";
        String[] splitLine = {};
        while (true) {
            System.out.println(
                    " - Чтобы завершить нажмите Enter\n" +
                    " - Ожидает команды от пользователя:\n"
            );
            try {
                args = CONSOLE.readLine();
                splitLine = args.trim().split("\\s+");
                System.out.println(splitLine[0]);
            } catch (Exception e) {
                System.out.println(" Не корректный ввод: " + e.getMessage());
            }
            if (args.isBlank()) break;
            for (int i = 0; i < splitLine.length; i++) {
                switch (splitLine[i]) {
                    case "-q":
                        if (readyMode){
                            readFiles();
                        }else {
                            System.out.println("Предупреждение: Нет фалов для фильтрации ");
                        }

                        break;
                    case "-e":
                        parseFile();
                        break;
                    case "-o":
                        if (i + 1 < splitLine.length) {
                            outputPath = Paths.get(inputPath, splitLine[++i]).toString();
                        } else {
                            System.out.println("Предупреждение: Не прописан новый путь ");
                        }
                        break;
                    case "-p":
                        if (i + 1 < splitLine.length) {
                            prefix = splitLine[++i];
                        }else{
                            System.out.println("Предупреждение: Не прописан новый префикс ");
                        }
                        break;
                    case "-a":
                        appendMode = true;
                        break;
                    case "-s":
                        statsMode = "short";
                        break;
                    case "-f":
                        statsMode = "full";
                        break;
                    default:
                        System.out.println("Предупреждение: Неизвестная команда " + args + "\n" + "Чтобы завершить нажмите Enter");
                        break;
                }
            }
        }


    }

    private static void readFiles(){
        for (String fileName : listFiles) {
            String filePath = Paths.get(inputPath, fileName).toString();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ChangeType(line.trim());
                }
            } catch (FileNotFoundException e) {
                System.out.println("Файл не найден: " + fileName);
            } catch (IOException e) {
                System.out.println("Ошибка при чтении файла " + fileName + ": " + e.getMessage());
            }
        }
        closeWriters();
        printStatistics();
    }

    private static void ChangeType(String line) {
        if (line.isEmpty()) return;

        try {
            long val = Long.parseLong(line.trim());
            integerStats.addInt(val);
            writeToFile("integers.txt", line, "int");
            return;
        } catch (NumberFormatException ignored) {}

        try {
            double val = Double.parseDouble(line.trim().replace(',','.'));
            if (Double.isFinite(val)) {
                floatStats.addFloat(val);
                writeToFile("floats.txt", line, "float");
                return;
            }
        } catch (NumberFormatException ignored) {}

        stringStats.addString(line);
        writeToFile("strings.txt", line, "string");
    }

    private static void writeToFile(String baseName, String content, String type) {
        try {
            if (type.equals("int") && intWriter == null) intWriter = createWriter(baseName);
            if (type.equals("float") && floatWriter == null) floatWriter = createWriter(baseName);
            if (type.equals("string") && stringWriter == null) stringWriter = createWriter(baseName);

            BufferedWriter currentWriter = switch (type) {
                case "int" -> intWriter;
                case "float" -> floatWriter;
                default -> stringWriter;
            };

            currentWriter.write(content);
            currentWriter.newLine();
        } catch (IOException e) {
            System.out.println("Ошибка записи в " + baseName + ": " + e.getMessage());
        }
    }

    private static BufferedWriter createWriter(String baseName) throws IOException {
        Path path = Paths.get(outputPath,prefix + baseName);
        Files.createDirectories(path.getParent());
        return new BufferedWriter(new FileWriter(path.toString(), appendMode));
    }

    private static void closeWriters() {
        try {
            if (intWriter != null) { intWriter.close(); intWriter = null; }
            if (floatWriter != null) { floatWriter.close(); floatWriter = null; }
            if (stringWriter != null) { stringWriter.close(); stringWriter = null; }
        } catch (IOException e) {
            System.out.println("Ошибка при закрытии файлов: " + e.getMessage());
        }
    }

    private static void printStatistics() {
        if (statsMode.equals("none")) return;
        integerStats.print(statsMode);
        floatStats.print(statsMode);
        stringStats.print(statsMode);
    }

}

// Класс для сбора статистики
final class Stats {
    String name;
    long count = 0;
    // Для чисел
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    double sum = 0;
    // Для строк
    int minLen = Integer.MAX_VALUE;
    int maxLen = 0;

    Stats(String name) {
        this.name = name;
    }

    void addInt(long val) {
        count++;
        if (val < min) min = val;
        if (val > max) max = val;
        sum += val;
    }

    void addFloat(double val) {
        count++;
        if (val < min) min = val;
        if (val > max) max = val;
        sum += val;
    }

    void addString(String s) {
        count++;
        if (s.length() < minLen) minLen = s.length();
        if (s.length() > maxLen) maxLen = s.length();
    }

    void print(String mode) {
        if (count == 0) return;
        System.out.println("--- Статистика для " + name + " ---");
        System.out.println("Количество элементов: " + count);
        if (mode.equals("full")) {
            if (name.equals("Strings")) {
                System.out.println("Мин. длина: " + minLen);
                System.out.println("Макс. длина: " + maxLen);
            } else {
                System.out.println("Минимум: " + min);
                System.out.println("Максимум: " + max);
                System.out.println("Сумма: " + sum);
                System.out.println("Среднее: " + (sum / count));
            }
        }
        System.out.println();
    }
}