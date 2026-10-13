package si.konferenca.registration.security;

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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects request bodies larger than the limit with 413 (SR-03). */
public class BodySizeLimitFilter extends OncePerRequestFilter {

  static final String MESSAGE = "The request is too large.";

  private final long maxBytes;

  public BodySizeLimitFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long declared = request.getContentLengthLong();
    if (declared > maxBytes) {
      ErrorResponses.write(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, MESSAGE);
      return;
    }
    if (declared >= 0) {
      chain.doFilter(request, response);
      return;
    }
    byte[] body = request.getInputStream().readNBytes((int) maxBytes + 1);
    if (body.length > maxBytes) {
      ErrorResponses.write(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, MESSAGE);
      return;
    }
    chain.doFilter(new CachedBodyRequest(request, body), response);
  }

  /** A request whose body was already read into memory. */
  static final class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream in = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return in.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return in.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("Asynchronous reading is not supported");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      return new BufferedReader(
          new InputStreamReader(
              getInputStream(),
              encoding == null
                  ? StandardCharsets.UTF_8
                  : java.nio.charset.Charset.forName(encoding)));
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
