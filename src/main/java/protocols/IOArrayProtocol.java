package protocols;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;


public class IOArrayProtocol {

    public static int[] read(DataInputStream input) throws IOException {
        int size = input.readInt();
        byte[] bytes = input.readNBytes(size);
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

    public static int[] toIntArray(ByteBuffer buffer) throws IOException {
        ArrayProtocol.Array message = ArrayProtocol.Array.parseFrom(buffer);
        return toArray(message.getArrList());
    }

    public static byte[] toByteArray(int[] array) {
        ArrayProtocol.Array.Builder builder = ArrayProtocol.Array.newBuilder();
        for (int element : array) {
            builder.addArr(element);
        }
        return builder.build().toByteArray();
    }

    public static ByteBuffer toByteBuffer(int[] array) {
        byte[] bytes = toByteArray(array);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    private static int[] toArray(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

}
