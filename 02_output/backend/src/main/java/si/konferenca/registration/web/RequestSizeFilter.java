package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects request bodies above the limit with 413, by header and by bytes read (SR-03). */
public class RequestSizeFilter extends OncePerRequestFilter {

  private final long maxBytes;

  public RequestSizeFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long declared = request.getContentLengthLong();
    if (declared > maxBytes) {
      Problems.write(response, HttpStatus.PAYLOAD_TOO_LARGE);
      return;
    }
    if (declared >= 0) {
      chain.doFilter(request, response);
      return;
    }
    InputStream in = request.getInputStream();
    byte[] body = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 1L, maxBytes + 1));
    if (body.length > maxBytes) {
      Problems.write(response, HttpStatus.PAYLOAD_TOO_LARGE);
      return;
    }
    chain.doFilter(new CachedBodyRequest(request, body), response);
  }

  /** A request whose body was read up front. */
  private static final class CachedBodyRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream source = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return source.read();
        }

        @Override
        public boolean isFinished() {
          return source.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("synchronous reads only");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
      return body.length;
    }

    @Override
    public long getContentLengthLong() {
      return body.length;
    }
  }
}
