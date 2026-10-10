package si.konferenca.registration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.api.PayloadTooLargeException;

/**
 * Refuses request bodies above the configured size (SR-03): by declared length up front, and by
 * counting bytes read for bodies without a length.
 */
final class RequestSizeFilter extends OncePerRequestFilter {

  private final long maxBytes;

  RequestSizeFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      ErrorResponseWriter.write(
          response, 413, "PAYLOAD_TOO_LARGE", "The request body is too large.");
      return;
    }
    chain.doFilter(new LimitedRequest(request, maxBytes), response);
  }

  private static final class LimitedRequest extends HttpServletRequestWrapper {
    private final long maxBytes;
    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream(), maxBytes);
      }
      return stream;
    }
  }

  private static final class LimitedStream extends ServletInputStream {
    private final ServletInputStream delegate;
    private final long maxBytes;
    private long read;

    LimitedStream(ServletInputStream delegate, long maxBytes) {
      this.delegate = delegate;
      this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b >= 0) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int n) throws PayloadTooLargeException {
      read += n;
      if (read > maxBytes) {
        throw new PayloadTooLargeException();
      }
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      delegate.setReadListener(listener);
    }
  }
}
