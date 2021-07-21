import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Logger;

public class Base64Encoder {

    private static final Logger LOGGER = Logger.getLogger("Base64Encoder");

    public static String Base64Encoder(String str){
        byte[] src = str.getBytes();
        return new String(Base64.getEncoder().encode(src));
    }

    public static byte[] ByteBase64Encoder(byte[] bytes){
        return Base64.getEncoder().encode(bytes);
    }

    public static void main(String[] args) {
        Scanner inputFromConsole = new Scanner(System.in);
        String filename = inputFromConsole.nextLine();
        FileOutputStream outputToFile = null;
        try (FileInputStream inputFromFile = new FileInputStream(filename)) {
            filename = inputFromConsole.nextLine();
            outputToFile = new FileOutputStream(filename);
            byte[] src = new byte[inputFromFile.available()];
            inputFromFile.read(src);
            byte[] encodedBytes = Base64.getEncoder().encode(src);
            outputToFile.write(encodedBytes);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (outputToFile != null) {
                try {
                    outputToFile.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}