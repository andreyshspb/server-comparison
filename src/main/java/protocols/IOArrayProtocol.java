package protocols;

import java.io.*;
import java.util.List;


public class IOArrayProtocol {

    public static int[] read(DataInputStream input) throws IOException {
        int size = input.readInt();
        byte[] bytes = new byte[size];

        int read = input.read(bytes);
        if (read != size) {
            throw new IOException("incorrect protocol message");
        }

        return toIntArray(bytes);
    }

    public static void write(DataOutputStream output, int[] array) throws IOException {
        byte[] serialized = toByteArray(array);
        output.writeInt(serialized.length);
        output.write(serialized);
        output.flush();
    }

    public static int[] toIntArray(byte[] data) throws IOException {
        ArrayProtocol.Array message = ArrayProtocol.Array.parseFrom(data);
        return toArray(message.getArrList());
    }

    public static byte[] toByteArray(int[] array) {
        ArrayProtocol.Array.Builder builder = ArrayProtocol.Array.newBuilder();
        for (int element : array) {
            builder.addArr(element);
        }
        return builder.build().toByteArray();
    }

    private static int[] toArray(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

}
