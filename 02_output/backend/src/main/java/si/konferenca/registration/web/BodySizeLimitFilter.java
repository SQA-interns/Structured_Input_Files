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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.config.AppProperties;

/**
 * SR-03: request bodies under /api are limited to {@code MAX_BODY_BYTES}; larger bodies get 413,
 * including bodies sent without a length (chunked).
 */
@Component
@Order(FilterOrder.BODY_SIZE)
public class BodySizeLimitFilter extends OncePerRequestFilter {

  static final String MESSAGE = "The request is too large.";

  private final int maxBytes;

  public BodySizeLimitFilter(AppProperties properties) {
    this.maxBytes = properties.limits().maxBodyBytes();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/")
        || "GET".equals(request.getMethod())
        || "HEAD".equals(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      ErrorBody.write(response, 413, MESSAGE);
      return;
    }
    byte[] body;
    try (InputStream in = request.getInputStream()) {
      body = in.readNBytes(maxBytes + 1);
    }
    if (body.length > maxBytes) {
      ErrorBody.write(response, 413, MESSAGE);
      return;
    }
    chain.doFilter(new CachedBodyRequest(request, body), response);
  }

  /** The request with its already read body. */
  static final class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
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
          throw new UnsupportedOperationException("Synchronous reads only");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      return new BufferedReader(
          new InputStreamReader(
              new ByteArrayInputStream(body),
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
