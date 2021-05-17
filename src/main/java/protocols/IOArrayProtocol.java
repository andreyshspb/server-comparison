package protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;


public class IOArrayProtocol {

    public static int[] read(DataInputStream input) throws IOException {
        int size = input.readInt();
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = input.readByte();
        }
        return toIntArray(bytes);
    }

    public static void write(DataOutputStream output, int[] array) throws IOException {
        output.write(toByteArray(array));
        output.flush();
    }

    private static int[] toIntArray(byte[] data) throws IOException {
        ArrayProtocol.Array message = ArrayProtocol.Array.parseFrom(data);
        return toArray(message.getArrList());
    }

    private static byte[] toByteArray(int[] array) {
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