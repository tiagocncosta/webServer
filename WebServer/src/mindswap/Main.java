package mindswap;

import mindswap.messages.Messages;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {


    private ServerSocket serverSocket;
    private ExecutorService service;

    public static void main(String[] args) {
        // write your code here
        int port = 8082;

        if (System.getenv("PORT") != null) {
            port = Integer.parseInt(System.getenv("PORT"));
        }
        try {
            new Main().start(port);
        } catch (IOException e) {
            System.err.println(Messages.SERVER_ERROR);
        }
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
        System.out.printf(Messages.SERVER_CONNECTED, serverSocket.getInetAddress().getHostAddress(), port);
        serveRequests(serverSocket, service);
    }

    private void serveRequests(ServerSocket serverSocket, ExecutorService service) {

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                service.submit(new RequestHandler(clientSocket));
            } catch (IOException e) {
                System.err.println(Messages.CLIENT_CONNECTION_ERROR);
            }
        }
    }

    private class RequestHandler implements Runnable {
        DataOutputStream writer;
        BufferedReader reader;
        Socket clientSocket;

        public RequestHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                writer = new DataOutputStream(clientSocket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println(Messages.CLIENT_CONNECTION_ERROR);
            }
        }

        public static String ok(String fileName, long length) {
            return "HTTP/1.0 200 Document Follows\r\n" +
                    contentType(fileName) +
                    "Content-Length: " + length + "\r\n\r\n";
        }

        private static String contentType(String fileName) {
            String contentType = "";
            try {
                contentType = Files.probeContentType(Path.of("www" + fileName));
            } catch (IOException e) {

            }
            return "Content-Type: " + contentType + "; charset=UTF-8\r\n";
        }

        @Override
        public void run() {
            dealWithRequest();
        }

        private void dealWithRequest() {
            try {
                String header = reader.readLine();
                String[] headers = splitHeader(header);
                System.out.printf(Messages.REQUEST, headers[0]);
                getResource(header);
               /*   List<String> headerLeft = new ArrayList<>();
              String line;
                while ((line = reader.readLine()) != "") {
                    System.out.println(line);
                    headerLeft.add(line);
                }
                System.out.println(headerLeft);*/

                // writer.writeBytes(Messages.SERVER_CONNECTED);
                // writer.flush();
            } catch (IOException e) {
                System.err.println(Messages.CLIENT_CONNECTION_ERROR);
            }
        }

        private void getResource(String header) {
            String[] headerParts = splitHeader(header);

            System.out.printf(Messages.REQUEST, headerParts[0]);
            String protocol = headerParts[2];
            String resource = headerParts[1]; // resource / or /index.html or /images/image.png
            String httpVerb = headerParts[0]; //GET or PUT or POST
            reply(httpVerb, resource);
        }

        private void reply(String httpVerb, String resource) {
            try {
                if (httpVerb.equals("GET")) {

                    //writer.write("<html><body><h1>Hello World</h1></body></html>");
                    resource = resource.equals("/") ? "/Users/tiagocosta/Documents/Mindera_exercises/Academy/Bootcamp/WebServer/src/www/hi.html" : resource;

                    if (Files.exists(Path.of(resource))) {
                        replyWithFile(new File(resource));
                    } else {
                        writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
                        writer.writeBytes("Content-Type: text/html\r\n");
                        writer.writeBytes("\r\n");
                        writer.writeBytes("<html><body><h1>Hummm not found</h1></body></html>");
                    }
                } else {
                    writer.writeBytes("HTTP/1.1 405 Not Allowed\r\n");
                    writer.writeBytes("Content-Type: text/html\r\n");
                    writer.writeBytes("\r\n");
                    writer.writeBytes("<html><body><h1>Hummm not found</h1></body></html>");
                }

                writer.flush();

            } catch (IOException e) {
                System.err.println(Messages.CLIENT_CONNECTION_ERROR);
            }
        }

        private String[] splitHeader(String header) {
            String[] headerParts = header.split(" ");
            return headerParts;
        }

        private void replyWithFile(File file) throws IOException {
            byte[] bytes = Files.readAllBytes(Path.of(file.getPath()));
            replyWithHeader(ok(file.getPath(), file.length()));
            writer.write(bytes);
        }

        private void replyWithHeader(String header) throws IOException {
            writer.writeBytes(header);
        }

    }
}
