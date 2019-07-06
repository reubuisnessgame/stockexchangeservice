package com.github.reubuisnessgame.gamebank.stockexchangeservice.security.jwt;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ExceptionModel;
import com.google.gson.Gson;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;

public class JwtTokenFilter extends GenericFilterBean {

    private JwtTokenProvider jwtTokenProvider;

    private PrintWriter writer;

    JwtTokenFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException, InvalidJwtAuthenticationException {

        String token = jwtTokenProvider.resolveToken((HttpServletRequest) req);
        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication auth = null;
                try {
                    auth = jwtTokenProvider.getAuthentication(token);
                } catch (IllegalAccessException ignore) {
                }

                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (InvalidJwtAuthenticationException | UsernameNotFoundException e) {
            ExceptionModel ex = new ExceptionModel(403, "Forbidden",
                    e.getMessage(), ((HttpServletRequest) req).getRequestURI());
            Gson gson = new Gson();
            String json = gson.toJson(ex);
            if (writer == null) {
                writer = res.getWriter().append(json);
            }
        }
        filterChain.doFilter(req, res);
    }

}
