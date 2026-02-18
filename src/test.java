import java.util.ArrayList;
import java.util.List;

public class test {
    private static final List<String> listFiles = new ArrayList<>();
    public static void main(String[] args) {
        listFiles.add("1");
        listFiles.add("2");
        listFiles.remove("1");
        System.out.println(listFiles);
    }
}
