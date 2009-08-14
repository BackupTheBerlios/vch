package de.berlios.vch.http.filter;

import java.io.IOException;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class OutputStreamDebugFilter extends Filter {

    @Override
    public String description() {
        return "debug filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        exchange.setStreams(exchange.getRequestBody(), new SniffingOutputStream(exchange.getResponseBody()));
        chain.doFilter(exchange);
    }

}
