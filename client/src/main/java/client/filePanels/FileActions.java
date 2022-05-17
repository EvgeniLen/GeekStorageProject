package client.filePanels;

import service.serializedClasses.FileResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileActions {
    private static final Map<String, FileChannel> mapFileChannel = new HashMap<>();
    private static final int MB_5 = 5 * 1_000_000;
    public static FileChannel getFileChannel(String path) throws FileNotFoundException {
        FileChannel fileChannel = mapFileChannel.get(path);
        if (fileChannel == null) {
            RandomAccessFile fileForSend = new RandomAccessFile(path, "rw");
            fileChannel = fileForSend.getChannel();
            mapFileChannel.put(path, fileChannel);
        }
        return fileChannel;
    }

    public static boolean getServerFile(FileResponse response){
        try {
            Path path = Path.of(response.getLocalPath());
            if (response.getFileInfo().getType().getName().equals("D")){
                Files.createDirectories(path);
            } else {
                ByteBuffer byteBuffer = ByteBuffer.wrap(response.getFile());
                FileChannel fileChannel = null;
                if (byteBuffer.hasRemaining()){
                    fileChannel = getFileChannel(path.toString());
                    fileChannel.write(byteBuffer);
                    byteBuffer.clear();
                }

                if (response.getFileInfo().getSize() == Files.size(path)){
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                    mapFileChannel.remove(path.toString());
                    return true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return false;
    }

    public static void deleteFiles(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } else {
            Files.delete(path);
        }
    }

    public static void splitFile(Path path, Consumer<byte[]> filePartCons) {
        byte[] bytes = new byte[0];
        try {
            if (!path.toFile().isDirectory()) {
                RandomAccessFile fileForSend = null;

                fileForSend = new RandomAccessFile(path.toFile(), "rw");

                ByteBuffer byteBuffer = ByteBuffer.allocate(MB_5);
                FileChannel fileChannel = fileForSend.getChannel();
                while (fileChannel.read(byteBuffer) != -1) {
                    bytes = new byte[byteBuffer.flip().remaining()];
                    byteBuffer.get(bytes);
                    filePartCons.accept(bytes);
                    byteBuffer.clear();
                }
                fileForSend.close();
            } else {
                filePartCons.accept(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
