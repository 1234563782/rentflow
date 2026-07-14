import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class GenerateDevKeys {
    private GenerateDevKeys() {
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "keys" : args[0]).toAbsolutePath();
        Files.createDirectories(output);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();

        Files.writeString(
                output.resolve("private.pem"),
                pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()),
                StandardCharsets.US_ASCII
        );
        Files.writeString(
                output.resolve("public.pem"),
                pem("PUBLIC KEY", keyPair.getPublic().getEncoded()),
                StandardCharsets.US_ASCII
        );
        System.out.println("Generated RSA development keys in " + output);
    }

    private static String pem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n"
                + body
                + "\n-----END " + type + "-----\n";
    }
}
