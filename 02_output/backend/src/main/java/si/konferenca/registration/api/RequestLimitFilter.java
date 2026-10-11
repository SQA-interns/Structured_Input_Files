package si.konferenca.registration.api;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rate limits per client and endpoint group, and the request-body size limit on /api (SR-03). */
public final class RequestLimitFilter extends OncePerRequestFilter {

  static final int TOO_MANY_REQUESTS = 429;

  /** Requests per minute for the three public endpoint groups, and the body size limit. */
  public record Limits(int registration, int formConfig, int export, int maxRequestBytes) {}

  private final FixedWindowRateLimiter limiter;
  private final Limits limits;

  public RequestLimitFilter(FixedWindowRateLimiter limiter, Limits limits) {
    this.limiter = limiter;
    this.limits = limits;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String group = group(request.getRequestURI());
    if (group != null) {
      long retryAfter = limiter.acquire(group + "|" + request.getRemoteAddr(), limit(group));
      if (retryAfter > 0) {
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        ErrorWriter.write(response, TOO_MANY_REQUESTS, "Too many requests. Please wait a minute.");
        return;
      }
    }
    int max = limits.maxRequestBytes();
    if (request.getContentLengthLong() > max) {
      tooLarge(response);
      return;
    }
    byte[] body = request.getInputStream().readNBytes(max + 1);
    if (body.length > max) {
      tooLarge(response);
      return;
    }
    chain.doFilter(new CachedBodyRequest(request, body), response);
  }

  private static void tooLarge(HttpServletResponse response) throws IOException {
    ErrorWriter.write(
        response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request too large.");
  }

  private static String group(String uri) {
    return switch (uri) {
      case "/api/registrations" -> "registration";
      case "/api/form-config" -> "form-config";
      case "/api/export" -> "export";
      default -> null;
    };
  }

  private int limit(String group) {
    return switch (group) {
      case "registration" -> limits.registration();
      case "form-config" -> limits.formConfig();
      default -> limits.export();
    };
  }

  /** The request with its already read body. */
  private static final class CachedBodyRequest extends HttpServletRequestWrapper {

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
        public boolean isFinished() {
          return in.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("synchronous reads only");
        }

        @Override
        public int read() {
          return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return in.read(b, off, len);
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }
}
