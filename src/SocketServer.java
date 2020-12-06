import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private int ThreadNums = 3;
    private int currentNum = 0;
    private ServerSocket serverSocket;
    private ExecutorService executorService = Executors.newFixedThreadPool(ThreadNums);

    public static void main(String[] args) {
        SocketServer server = new SocketServer(7000);
        System.out.println("Server started!\n");
        server.start();
    }

    public SocketServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("exception in constructor");
        }
    }

    private Runnable task(Socket socket) {
        return () -> {
            BufferedReader bufferedReader;
            OutputStream outputStream;
            PrintWriter writer;

            try {
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = socket.getOutputStream();
                writer = new PrintWriter(outputStream, true);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if ("".equals(line))
                        continue;
                    if ("quit".equals(line))
                        break;

                    String[] str = line.split(" ");
                    if (str.length < 3 || !str[2].equals("HTTP/1.1")) {
                        writer.write("HTTP/1.1 400 Bad Request\n\r");
                        writer.flush();
                        continue;
                    }

                    String path = str[1];
                    if (path.equals("/"))
                        path += "test1.html";//test1.html is our index page

                    String ext = path.split("[.]")[1];

                    if (ext.equals("txt") || ext.equals("html"))
                        ext = "txt/html";

                    URL url = SocketServer.class.getResource(path);
                    if (url != null) {
                        InputStream stream = url.openStream();

                        byte[] bytes = stream.readAllBytes();
                        writer.write("HTTP/1.1 200 OK\n\r");
                        writer.flush();
                        writer.write("Host: localhost:7000\n\r");
                        writer.flush();
                        writer.write("Connection: keep-alive\n\r");
                        writer.flush();
                        writer.write("Content-Length: " + bytes.length + "\n\r");
                        writer.flush();
                        writer.write("Content-Type: " + ext + "; charset=UTF-8\n\r\n\r");
                        writer.flush();

                        if ("GET".equals(str[0])) {
                            outputStream.write(bytes);
                            outputStream.flush();
                        } else if ("HEAD".equals(str[0])) {
                            //headers are already written
                        } else {
                            writer.write("This operation is not supported\n\r");
                            writer.flush();
                        }
                    } else {
                        writer.write("HTTP/1.1 404 Not Found\n\r");
                        writer.flush();
                        writer.write("Content-Type: " + ext + "; charset=UTF-8\n\r");
                        writer.flush();
                    }
                    writer.flush();
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writer.flush();
            }
            currentNum--;
            System.out.println("Connection closed : " + socket.toString());
        };
    }

    public void start() {
        while (true) {
            try {
                if (currentNum < ThreadNums) {
                    //Listens for a connection to be made to this socket and accepts it.
                    //The method is blocked until a connection is made.
                    Socket socket = serverSocket.accept();
                    currentNum++;
                    System.out.println("Client connected : " + socket.toString());
                    executorService.submit(task(socket));
                }
                System.out.println(currentNum);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
