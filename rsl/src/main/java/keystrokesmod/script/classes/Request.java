package keystrokesmod.script.classes;

import keystrokesmod.utility.NetworkUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Request {
    public String method;
    public String url;
    public List<String[]> headers = new ArrayList<>();
    public String userAgent;
    public int connectionTimeout;
    public int readTimeout;
    public String content = "";

    public Request(String method, String URL) {
        if (!method.equals("POST") && !method.equals("GET")) {
            this.method = "GET";
        }
        else {
            this.method = method;
        }
        this.url = URL;
        this.userAgent = "";
        this.readTimeout = 5000;
        this.connectionTimeout = 5000;

    }

    public void addHeader(final String header, final String value) {
        if (this.headers == null) {
            this.headers = new ArrayList<>();
        }
        this.headers.add(new String[] { header, value });
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setConnectTimeout(int timeout) {
        this.connectionTimeout = timeout;
    }

    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Response fetch() {
        if (!this.url.isEmpty()) {
            HttpURLConnection con = null;
            try {
                final URL url = new URL(this.url);
                con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod(this.method);
                con.setConnectTimeout(this.connectionTimeout);
                con.setReadTimeout(this.readTimeout);
                con.setRequestProperty("User-Agent", this.userAgent.isEmpty() ? NetworkUtils.CHROME_USER_AGENT : this.userAgent);
                if (this.headers != null && !this.headers.isEmpty()) {
                    for (final String[] header : this.headers) {
                        con.setRequestProperty(header[0], header[1]);
                    }
                }
                if (this.method.equals("POST") && !this.content.isEmpty()) {
                    con.setDoOutput(true);
                    final byte[] out = this.content.getBytes(StandardCharsets.UTF_8);
                    con.setFixedLengthStreamingMode(out.length);
                    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    con.connect();
                    final OutputStream os = con.getOutputStream();
                    try {
                        os.write(out);
                        if (os != null) {
                            os.close();
                        }
                    }
                    catch (Throwable t) {
                        if (os != null) {
                            try {
                                os.close();
                            }
                            catch (Throwable t2) {
                                t.addSuppressed(t2);
                            }
                        }
                        throw t;
                    }
                }
                String contents = "";
                try {
                    final BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    try {
                        final StringBuilder sb = new StringBuilder();
                        String input;
                        while ((input = br.readLine()) != null) {
                            sb.append(input);
                        }
                        contents = sb.toString();
                        br.close();
                    }
                    catch (Throwable t3) {
                        try {
                            br.close();
                        }
                        catch (Throwable t4) {
                            t3.addSuppressed(t4);
                        }
                        throw t3;
                    }
                }
                catch (IOException er1) {
                    InputStream errorStream = con.getErrorStream();
                    if (errorStream != null) {
                        try {
                            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                            try {
                                final StringBuilder sb2 = new StringBuilder();
                                String input2;
                                while ((input2 = errorReader.readLine()) != null) {
                                    sb2.append(input2);
                                }
                                contents = sb2.toString();
                                errorReader.close();
                            }
                            catch (Throwable t5) {
                                try {
                                    errorReader.close();
                                }
                                catch (Throwable t6) {
                                    t5.addSuppressed(t6);
                                }
                                throw t5;
                            }
                        }
                        catch (IOException ex) {}
                    }
                }
                return new Response(con.getResponseCode(), contents);
            }
            catch (IOException ex2) {}
            finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Request(" + this.method + "," + this.url + ")";
    }
}
