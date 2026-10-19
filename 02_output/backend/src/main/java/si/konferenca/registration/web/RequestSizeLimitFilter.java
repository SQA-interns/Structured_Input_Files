package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/** SR-03: request bodies larger than the limit are refused with 413. */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  private final long maxBytes;

  public RequestSizeLimitFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      response.setContentType("application/json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write("{\"message\":\"The request is too large.\",\"fieldErrors\":[]}");
      return;
    }
    chain.doFilter(new LimitedRequest(request, maxBytes), response);
  }

  /** Counts the bytes of a body without a declared length and stops at the limit. */
  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private final long maxBytes;

    LimitedRequest(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      ServletInputStream body = super.getInputStream();
      return new ServletInputStream() {
        private long read;

        @Override
        public int read() throws IOException {
          int next = body.read();
          if (next >= 0 && ++read > maxBytes) {
            throw new IOException("request body too large");
          }
          return next;
        }

        @Override
        public boolean isFinished() {
          return body.isFinished();
        }

        @Override
        public boolean isReady() {
          return body.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
          body.setReadListener(listener);
        }
      };
    }
  }
}
